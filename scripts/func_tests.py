import os.path as osp
import unittest
from confreader import read_conf_into_dict

from org.apache.log4j import BasicConfigurator
from org.apache.log4j.varia import NullAppender
from java.lang import NullPointerException

from org.continuousassurance.swamp.cli import SwampApiWrapper
from org.continuousassurance.swamp.cli.exceptions import InvalidIdentifierException
from org.continuousassurance.swamp.cli.exceptions import IncompatibleAssessmentTupleException
from org.continuousassurance.swamp.session import HTTPException
from edu.uiuc.ncsa.security.core.exceptions import GeneralException


class TestSwampApiWrapper(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        try:
            config_file = osp.join(osp.dirname(__file__), 'resources/development.properties')
            user_conf = read_conf_into_dict(config_file)
        except IOError as err:
            print('''Please create "%s" with
            USERNAME=<swamp-username>
            PASSWORD=<swamp-password>
            PROJECT=<test-project-uuid>
            ''' % config_file)
            raise err
        TestSwampApiWrapper.USERNAME = user_conf['USERNAME']
        TestSwampApiWrapper.PASSWORD = user_conf['PASSWORD']
        # Please enter your default project
        TestSwampApiWrapper.PROJECT = user_conf.get('PROJECT',
                                                    '5bf4d93c-2945-42d0-9311-6507518219f3')
        TestSwampApiWrapper.HOST = 'https://dt.cosalab.org'
        #TestSwampApiWrapper.HOST = 'https://www.mir-swamp.org'


class TestLogin(TestSwampApiWrapper):

    @classmethod
    def setUpClass(cls):
        super(TestLogin, cls).setUpClass()

    def setUp(self):
        self.api_wrapper = SwampApiWrapper(TestSwampApiWrapper.HOST)

    def test_login(self):
        self.assertNotEqual(self.api_wrapper.login(TestSwampApiWrapper.USERNAME,
                                                   TestSwampApiWrapper.PASSWORD),
                            None, "Login Failed")

    def test_login_incorrect(self):
        self.assertRaises(HTTPException, self.api_wrapper.login,
                          TestSwampApiWrapper.USERNAME,
                          TestSwampApiWrapper.PASSWORD[:-1])

    @unittest.expectedFailure
    def test_login_incorrect2(self):
        self.api_wrapper.setHost('https://it.cosalab.org/')
        self.assertRaises(GeneralException, self.api_wrapper.login,
                          TestSwampApiWrapper.USERNAME,
                          TestSwampApiWrapper.PASSWORD)


class TestProjects(TestSwampApiWrapper):

    @classmethod
    def setUpClass(cls):
        super(TestProjects, cls).setUpClass()

    def setUp(self):
        self.api_wrapper = SwampApiWrapper(TestSwampApiWrapper.HOST)
        self.api_wrapper.login(TestSwampApiWrapper.USERNAME, TestSwampApiWrapper.PASSWORD)

    def test_get_projects(self):
        proj_list = self.api_wrapper.getProjectsList()
        self.assertIsNotNone(proj_list)

    def test_get_projects_fail1(self):
        self.api_wrapper.logout()
        self.assertRaises(NullPointerException, self.api_wrapper.getProjectsList)


class TestTools(TestSwampApiWrapper):

    @classmethod
    def setUpClass(cls):
        super(TestTools, cls).setUpClass()

    def setUp(self):
        self.api_wrapper = SwampApiWrapper(TestSwampApiWrapper.HOST)
        self.api_wrapper.login(TestSwampApiWrapper.USERNAME, TestSwampApiWrapper.PASSWORD)

    @unittest.skip('Does not work because it is protected')
    def test_get_tools(self):
        tool_map = self.api_wrapper.getAllTools(TestSwampApiWrapper.PROJECT)
        self.assertIsNotNone(tool_map)
        for tool in tool_map.values():
            print("%-21s, %-38s, %s\n", tool.getName(),
                  tool.getSupportedPkgTypes(),
                  tool.getSupportedPlatforms())

    def test_get_tools_supported(self):
        tool_list = self.api_wrapper.getTools("C/C++", TestSwampApiWrapper.PROJECT)
        self.assertIsNotNone(tool_list)


class TestPlatforms(TestSwampApiWrapper):

    @classmethod
    def setUpClass(cls):
        super(TestPlatforms, cls).setUpClass()

    def setUp(self):
        self.api_wrapper = SwampApiWrapper(TestSwampApiWrapper.HOST)
        self.api_wrapper.login(TestSwampApiWrapper.USERNAME,
                               TestSwampApiWrapper.PASSWORD)

    def test_get_platforms(self):
        plat_map = self.api_wrapper.getAllPlatforms()
        self.assertIsNotNone(plat_map)

    def test_get_plats_supported(self):
        tool_uuid = '738b81f0-a828-11e5-865f-001a4a81450b'
        plat_list = self.api_wrapper.getSupportedPlatforms(tool_uuid,
                                                           TestSwampApiWrapper.PROJECT)
        for plat in plat_list:
            print(plat)
        self.assertIsNotNone(plat_list)


class TestUpload(TestSwampApiWrapper):

    @classmethod
    def setUpClass(cls):
        super(TestUpload, cls).setUpClass()
        cls.PKG_CONF = osp.join(osp.dirname(__file__),
                                'resources/packages/swamp-gradle-example-1.0/package.conf')
        cls.PKG_ARCHIVE = osp.join(osp.dirname(__file__),
                                   'resources/packages/swamp-gradle-example-1.0/swamp-gradle-example-1.0.zip')

    def setUp(self):
        self.api_wrapper = SwampApiWrapper(TestSwampApiWrapper.HOST)
        self.api_wrapper.login(TestSwampApiWrapper.USERNAME,
                               TestSwampApiWrapper.PASSWORD)

    def test_get_pkg_types(self):
        pkg_list = self.api_wrapper.getPackageTypesList()
        self.assertIsNotNone(pkg_list)

    def test_get_pkg_list(self):
        pkg_list = self.api_wrapper.getPackagesList(None)
        self.assertIsNotNone(pkg_list)

    def test_get_pkg_list_from_project(self):
        pkg_list = self.api_wrapper.getPackagesList(TestSwampApiWrapper.PROJECT)
        self.assertIsNotNone(pkg_list)

    def test_upload_new_pkg(self):
        pkg_uuid = self.api_wrapper.uploadPackage(TestUpload.PKG_CONF,
                                                  TestUpload.PKG_ARCHIVE,
                                                  TestSwampApiWrapper.PROJECT,
                                                  True)
        self.assertIsNotNone(pkg_uuid)

    def test_upload_pkg_ver(self):
        pkg_uuid = self.api_wrapper.uploadPackage(TestUpload.PKG_CONF,
                                                  TestUpload.PKG_ARCHIVE,
                                                  TestSwampApiWrapper.PROJECT,
                                                  False)
        self.assertIsNotNone(pkg_uuid)

    def test_upload_pkg_ver_fail1(self):
        # Incorrect project uuid
        self.assertRaises(InvalidIdentifierException,
                          self.api_wrapper.uploadPackage,
                          TestUpload.PKG_CONF,
                          TestUpload.PKG_ARCHIVE,
                          'd47380ea-a4ef-0a88-0a17-aab43d80fdbe',
                          False)


class TestAssess(TestSwampApiWrapper):

    @classmethod
    def setUpClass(cls):
        super(TestAssess, cls).setUpClass()
        cls.PKG_CONF = osp.join(osp.dirname(__file__),
                                'resources/packages/swamp-gradle-example-1.0/package.conf')
        cls.PKG_ARCHIVE = osp.join(osp.dirname(__file__),
                               'resources/packages/swamp-gradle-example-1.0/swamp-gradle-example-1.0.zip')
        cls.API_WRAPPER = SwampApiWrapper(TestSwampApiWrapper.HOST)
        cls.API_WRAPPER.login(TestSwampApiWrapper.USERNAME,
                              TestSwampApiWrapper.PASSWORD)
        cls.PKG_VER_UUID = cls.API_WRAPPER.uploadPackage(cls.PKG_CONF,
                                                         cls.PKG_ARCHIVE,
                                                         TestSwampApiWrapper.PROJECT,
                                                         True)

    @classmethod
    def tearDownClass(cls):
        pkg_ver = cls.API_WRAPPER.getPackageVersion(cls.PKG_VER_UUID,
                                                    TestSwampApiWrapper.PROJECT)
        cls.API_WRAPPER.deletePackage(pkg_ver.getPackageThing().getUUIDString(),
                                      TestSwampApiWrapper.PROJECT)
        cls.API_WRAPPER.logout()

    def test_get_run_assess(self):
        self.assertIsNotNone(TestAssess.PKG_VER_UUID)

        arun_uuid = TestAssess.API_WRAPPER.runAssessment(TestAssess.PKG_VER_UUID,
                                                         '163f2b01-156e-11e3-a239-001a4a81450b',
                                                         TestSwampApiWrapper.PROJECT,
                                                         None)
        self.assertIsNotNone(arun_uuid)

    def test_get_run_assess_wrong_tool(self):
        self.assertIsNotNone(TestAssess.PKG_VER_UUID)

        # Using a python tool to assess Java package
        self.assertRaises(IncompatibleAssessmentTupleException,
                          TestAssess.API_WRAPPER.runAssessment,
                          TestAssess.PKG_VER_UUID,
                          '7fbfa454-8f9f-11e4-829b-001a4a81450b',
                          TestSwampApiWrapper.PROJECT,
                          None)


if __name__ == '__main__':
    BasicConfigurator.configure(NullAppender())
    unittest.main()
