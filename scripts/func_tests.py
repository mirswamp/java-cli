import os
import os.path as osp
import time
import unittest
from confreader import read_conf_into_dict

from org.apache.log4j import BasicConfigurator
from org.apache.log4j.varia import NullAppender
from java.lang import NullPointerException

from org.continuousassurance.swamp.cli import SwampApiWrapper
from org.continuousassurance.swamp.cli.exceptions import InvalidIdentifierException
from org.continuousassurance.swamp.cli.util import AssessmentStatus
from org.continuousassurance.swamp.session import HTTPException
from edu.uiuc.ncsa.security.core.exceptions import GeneralException


class TestSwampApiWrapper(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        try:
            #config_file = osp.join(osp.dirname(__file__),
            #'resources/development.properties')
            config_file = osp.join(osp.dirname(__file__),
                                   'resources/userinfo.properties')
            user_conf = read_conf_into_dict(config_file)
        except IOError as err:
            print('''Please create "%s" with
            username=<swamp-username>
            password=<swamp-password>
            project=<test-project-uuid>
            hostname=<swamp-hostname>
            ''' % config_file)
            raise err
        TestSwampApiWrapper.USERNAME = user_conf['username']
        TestSwampApiWrapper.PASSWORD = user_conf['password']
        # Please enter your default project
        TestSwampApiWrapper.PROJECT = user_conf['project']

        #TestSwampApiWrapper.HOST = 'https://dt.cosalab.org'
        TestSwampApiWrapper.HOST = user_conf.get('hostname',
                                                 'https://www.mir-swamp.org')


class TestLogin(TestSwampApiWrapper):

    @classmethod
    def setUpClass(cls):
        super(TestLogin, cls).setUpClass()

    def setUp(self):
        self.api_wrapper = SwampApiWrapper()

    def test_login(self):
        self.assertNotEqual(self.api_wrapper.login(TestSwampApiWrapper.USERNAME,
                                                   TestSwampApiWrapper.PASSWORD,
                                                   TestSwampApiWrapper.HOST),
                            None, "Login Failed")

    def test_login_incorrect(self):
        self.assertRaises(HTTPException, self.api_wrapper.login,
                          TestSwampApiWrapper.USERNAME,
                          TestSwampApiWrapper.PASSWORD[:-1],
                          TestSwampApiWrapper.HOST)

    @unittest.expectedFailure
    def test_login_incorrect2(self):
        #self.api_wrapper.setHost()
        self.assertRaises(GeneralException, self.api_wrapper.login,
                          TestSwampApiWrapper.USERNAME,
                          TestSwampApiWrapper.PASSWORD,
                          'https://it.cosalab.org/')


class TestProjects(TestSwampApiWrapper):

    @classmethod
    def setUpClass(cls):
        super(TestProjects, cls).setUpClass()

    def setUp(self):
        self.api_wrapper = SwampApiWrapper()
        self.api_wrapper.login(TestSwampApiWrapper.USERNAME,
                               TestSwampApiWrapper.PASSWORD,
                               TestSwampApiWrapper.HOST)

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
        self.api_wrapper = SwampApiWrapper()
        self.api_wrapper.login(TestSwampApiWrapper.USERNAME,
                               TestSwampApiWrapper.PASSWORD,
                               TestSwampApiWrapper.HOST)

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
        self.api_wrapper = SwampApiWrapper()
        self.api_wrapper.login(TestSwampApiWrapper.USERNAME,
                               TestSwampApiWrapper.PASSWORD,
                               TestSwampApiWrapper.HOST)

    def test_get_platforms(self):
        plat_map = self.api_wrapper.getAllPlatforms()
        self.assertIsNotNone(plat_map)

    def test_get_plats_supported(self):
        tool_uuid = '738b81f0-a828-11e5-865f-001a4a81450b'
        plat_list = self.api_wrapper.getSupportedPlatformVersions(tool_uuid,
                                                                  TestSwampApiWrapper.PROJECT)
        for plat in plat_list:
            print(plat)
        self.assertIsNotNone(plat_list)


class TestUpload(TestSwampApiWrapper):

    @classmethod
    def setUpClass(cls):
        super(TestUpload, cls).setUpClass()
        cls.PKG_LIST = list()
        cls.api_wrapper = SwampApiWrapper()
        cls.api_wrapper.login(TestSwampApiWrapper.USERNAME,
                              TestSwampApiWrapper.PASSWORD,
                              TestSwampApiWrapper.HOST)

    @classmethod
    def tearDownClass(cls):
        super(TestUpload, cls).setUpClass()
        for pkg_uuid in cls.PKG_LIST:
            cls.api_wrapper.deletePackage(pkg_uuid, TestSwampApiWrapper.PROJECT)

    def test_get_pkg_types(self):
        pkg_types = TestUpload.api_wrapper.getPackageTypesList()
        self.assertIsNotNone(pkg_types)

    def test_get_pkg_list(self):
        pkg_list = TestUpload.api_wrapper.getPackagesList(None)
        self.assertIsNotNone(pkg_list)

    def test_get_pkg_list_from_project(self):
        pkg_list = TestUpload.api_wrapper.getPackagesList(TestSwampApiWrapper.PROJECT)
        self.assertIsNotNone(pkg_list)

    def test_upload_new_pkg1(self):

        pkg_conf = osp.join(osp.dirname(__file__),
                            'resources/packages/swamp-gradle-example-1.0/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__),
                               'resources/packages/swamp-gradle-example-1.0/swamp-gradle-example-1.0.zip')

        self.pkg_uuid = TestUpload.api_wrapper.uploadPackage(pkg_conf,
                                                             pkg_archive,
                                                             TestSwampApiWrapper.PROJECT,
                                                             True)
        self.assertIsNotNone(self.pkg_uuid)
        TestUpload.PKG_LIST.append(self.pkg_uuid)

    def test_upload_new_pkg2(self):
        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/2048-android-1.8/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/2048-android-1.8/v1.8.zip')

        self.pkg_uuid = TestUpload.api_wrapper.uploadPackage(pkg_conf,
                                                             pkg_archive,
                                                             TestSwampApiWrapper.PROJECT,
                                                             True)
        self.assertIsNotNone(self.pkg_uuid)
        TestUpload.PKG_LIST.append(self.pkg_uuid)

    def test_upload_new_pkg3(self):
        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/beautifulsoup4-4.3.2/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/beautifulsoup4-4.3.2/beautifulsoup4-4.3.2.tar.gz')

        self.pkg_uuid = TestUpload.api_wrapper.uploadPackage(pkg_conf,
                                                             pkg_archive,
                                                             TestSwampApiWrapper.PROJECT,
                                                             True)
        self.assertIsNotNone(self.pkg_uuid)
        TestUpload.PKG_LIST.append(self.pkg_uuid)

    def test_upload_new_pkg4(self):
        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/capistrano-3.4.0/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/capistrano-3.4.0/capistrano-3.4.0.gem')

        self.pkg_uuid = TestUpload.api_wrapper.uploadPackage(pkg_conf,
                                                             pkg_archive,
                                                             TestSwampApiWrapper.PROJECT,
                                                             True)
        self.assertIsNotNone(self.pkg_uuid)
        TestUpload.PKG_LIST.append(self.pkg_uuid)

    def test_upload_new_pkg5(self):
        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/java-cli-1.3.0/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/java-cli-1.3.0/java-cli-1.1.zip')

        pkg_uuid = TestUpload.api_wrapper.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestUpload.PKG_LIST.append(pkg_uuid)

    def test_upload_new_pkg6(self):

        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/lighttpd-1.4.45/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/lighttpd-1.4.45/lighttpd-1.4.45.tar.xz')

        pkg_uuid = TestUpload.api_wrapper.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestUpload.PKG_LIST.append(pkg_uuid)

    def test_upload_new_pkg7(self):

        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/moodle-3.1.1/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/moodle-3.1.1/moodle-3.1.1.zip')

        pkg_uuid = TestUpload.api_wrapper.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestUpload.PKG_LIST.append(pkg_uuid)

    def test_upload_new_pkg8(self):

        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/pylxc-0.0.3/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/pylxc-0.0.3/pylxc-0.0.3.tar.gz')

        pkg_uuid = TestUpload.api_wrapper.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestUpload.PKG_LIST.append(pkg_uuid)

    def test_upload_new_pkg9(self):

        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/railsgoat-9052b4fcf0/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/railsgoat-9052b4fcf0/railsgoat-9052b4fcf0.zip')

        pkg_uuid = TestUpload.api_wrapper.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestUpload.PKG_LIST.append(pkg_uuid)

    def test_upload_new_pkg10(self):

        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/sandrorat-apk-unknown/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/sandrorat-apk-unknown/SandroRat.apk')

        pkg_uuid = TestUpload.api_wrapper.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestUpload.PKG_LIST.append(pkg_uuid)

    def test_upload_new_pkg11(self):

        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/scarf-io-1.0/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/scarf-io-1.0/scarf-io.zip')

        pkg_uuid = TestUpload.api_wrapper.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestUpload.PKG_LIST.append(pkg_uuid)

    def test_upload_new_pkg12(self):
        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/sinatra-starter-2ad9cba672/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/sinatra-starter-2ad9cba672/sinatra-starter-2ad9cba672.zip')

        pkg_uuid = TestUpload.api_wrapper.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestUpload.PKG_LIST.append(pkg_uuid)

    def test_upload_new_pkg13(self):

        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/swamp-gradle-example-1.0/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/swamp-gradle-example-1.0/swamp-gradle-example-1.0.zip')

        pkg_uuid = TestUpload.api_wrapper.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestUpload.PKG_LIST.append(pkg_uuid)

    def test_upload_new_pkg14(self):
        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/tomcat-coyote-7.0.27/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/tomcat-coyote-7.0.27/tomcat-coyote-7.0.27.tar.gz')

        pkg_uuid = TestUpload.api_wrapper.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestUpload.PKG_LIST.append(pkg_uuid)

    def test_upload_new_pkg15(self):
        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/wordpress-4.5.1/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/wordpress-4.5.1/WordPress-4.5.1.zip')

        pkg_uuid = TestUpload.api_wrapper.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestUpload.PKG_LIST.append(pkg_uuid)

    def test_upload_pkg_ver_fail1(self):
        # Incorrect project uuid
        self.assertRaises(InvalidIdentifierException,
                          TestUpload.api_wrapper.uploadPackage,
                          TestUpload.PKG_CONF,
                          TestUpload.PKG_ARCHIVE,
                          'd47380ea-a4ef-0a88-0a17-aab43d80fdbe',
                          False)


class TestAssess(TestSwampApiWrapper):

    @classmethod
    def setUpClass(cls):
        super(TestAssess, cls).setUpClass()

        cls.API_WRAPPER = SwampApiWrapper()
        cls.API_WRAPPER.login(TestSwampApiWrapper.USERNAME,
                              TestSwampApiWrapper.PASSWORD,
                              TestSwampApiWrapper.HOST)
        cls.PKG_LIST = list()

    @classmethod
    def tearDownClass(cls):
        try:
            for pkg_uuid in cls.PKG_LIST:
                pass
            #cls.API_WRAPPER.deletePackage(pkg_uuid, TestSwampApiWrapper.PROJECT)
        except InvalidIdentifierException as err:
            print(err)
        cls.API_WRAPPER.logout()

    def test_get_run_assess1(self):

        pkg_conf = osp.join(osp.dirname(__file__),
                            'resources/packages/swamp-gradle-example-1.0/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__),
                               'resources/packages/swamp-gradle-example-1.0/swamp-gradle-example-1.0.zip')

        pkg_uuid = TestAssess.API_WRAPPER.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestAssess.PKG_LIST.append(pkg_uuid)

        tool = TestAssess.API_WRAPPER.getToolFromName('error-prone',
                                                      TestSwampApiWrapper.PROJECT)
        arun_uuid = TestAssess.API_WRAPPER.runAssessment(pkg_uuid,
                                                         tool.getIdentifierString(),
                                                         TestSwampApiWrapper.PROJECT,
                                                         None)
        self.assertIsNotNone(arun_uuid)

    def test_get_run_assess2(self):
        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/2048-android-1.8/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/2048-android-1.8/v1.8.zip')

        pkg_uuid = TestAssess.API_WRAPPER.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestAssess.PKG_LIST.append(pkg_uuid)

        tool = TestAssess.API_WRAPPER.getToolFromName('Android lint',
                                                      TestSwampApiWrapper.PROJECT)
        arun_uuid = TestAssess.API_WRAPPER.runAssessment(pkg_uuid,
                                                         tool.getIdentifierString(),
                                                         TestSwampApiWrapper.PROJECT,
                                                         None)
        self.assertIsNotNone(arun_uuid)

    def test_get_run_assess3(self):
        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/beautifulsoup4-4.3.2/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/beautifulsoup4-4.3.2/beautifulsoup4-4.3.2.tar.gz')

        pkg_uuid = TestAssess.API_WRAPPER.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestAssess.PKG_LIST.append(pkg_uuid)

        tool = TestAssess.API_WRAPPER.getToolFromName('Bandit',
                                                      TestSwampApiWrapper.PROJECT)
        arun_uuid = TestAssess.API_WRAPPER.runAssessment(pkg_uuid,
                                                         tool.getIdentifierString(),
                                                         TestSwampApiWrapper.PROJECT,
                                                         None)
        self.assertIsNotNone(arun_uuid)

    def test_get_run_assess4(self):
        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/capistrano-3.4.0/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/capistrano-3.4.0/capistrano-3.4.0.gem')

        pkg_uuid = TestAssess.API_WRAPPER.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestAssess.PKG_LIST.append(pkg_uuid)

        tool = TestAssess.API_WRAPPER.getToolFromName('Reek',
                                                      TestSwampApiWrapper.PROJECT)
        arun_uuid = TestAssess.API_WRAPPER.runAssessment(pkg_uuid,
                                                         tool.getIdentifierString(),
                                                         TestSwampApiWrapper.PROJECT,
                                                         None)
        self.assertIsNotNone(arun_uuid)

    def test_get_run_assess5(self):
        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/java-cli-1.3.0/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/java-cli-1.3.0/java-cli-1.1.zip')

        pkg_uuid = TestAssess.API_WRAPPER.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestAssess.PKG_LIST.append(pkg_uuid)

        tool = TestAssess.API_WRAPPER.getToolFromName('Findbugs',
                                                      TestSwampApiWrapper.PROJECT)
        arun_uuid = TestAssess.API_WRAPPER.runAssessment(pkg_uuid,
                                                         tool.getIdentifierString(),
                                                         TestSwampApiWrapper.PROJECT,
                                                         None)
        self.assertIsNotNone(arun_uuid)

    def test_get_run_assess6(self):

        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/lighttpd-1.4.45/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/lighttpd-1.4.45/lighttpd-1.4.45.tar.xz')

        pkg_uuid = TestAssess.API_WRAPPER.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestAssess.PKG_LIST.append(pkg_uuid)

        tool = TestAssess.API_WRAPPER.getToolFromName('Clang Static Analyzer',
                                                      TestSwampApiWrapper.PROJECT)
        arun_uuid = TestAssess.API_WRAPPER.runAssessment(pkg_uuid,
                                                         tool.getIdentifierString(),
                                                         TestSwampApiWrapper.PROJECT,
                                                         None)
        self.assertIsNotNone(arun_uuid)

    def test_get_run_assess7(self):

        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/moodle-3.1.1/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/moodle-3.1.1/moodle-3.1.1.zip')

        pkg_uuid = TestAssess.API_WRAPPER.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestAssess.PKG_LIST.append(pkg_uuid)

        tool = TestAssess.API_WRAPPER.getToolFromName('ESLint',
                                                      TestSwampApiWrapper.PROJECT)
        arun_uuid = TestAssess.API_WRAPPER.runAssessment(pkg_uuid,
                                                         tool.getIdentifierString(),
                                                         TestSwampApiWrapper.PROJECT,
                                                         None)
        self.assertIsNotNone(arun_uuid)

    def test_get_run_assess8(self):

        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/pylxc-0.0.3/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/pylxc-0.0.3/pylxc-0.0.3.tar.gz')

        pkg_uuid = TestAssess.API_WRAPPER.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestAssess.PKG_LIST.append(pkg_uuid)

        tool = TestAssess.API_WRAPPER.getToolFromName('Pylint',
                                                      TestSwampApiWrapper.PROJECT)
        arun_uuid = TestAssess.API_WRAPPER.runAssessment(pkg_uuid,
                                                         tool.getIdentifierString(),
                                                         TestSwampApiWrapper.PROJECT,
                                                         None)
        self.assertIsNotNone(arun_uuid)

    def test_get_run_assess9(self):

        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/railsgoat-9052b4fcf0/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/railsgoat-9052b4fcf0/railsgoat-9052b4fcf0.zip')

        pkg_uuid = TestAssess.API_WRAPPER.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestAssess.PKG_LIST.append(pkg_uuid)

        tool = TestAssess.API_WRAPPER.getToolFromName('Brakeman',
                                                      TestSwampApiWrapper.PROJECT)
        arun_uuid = TestAssess.API_WRAPPER.runAssessment(pkg_uuid,
                                                         tool.getIdentifierString(),
                                                         TestSwampApiWrapper.PROJECT,
                                                         None)
        self.assertIsNotNone(arun_uuid)

    def test_get_run_assess10(self):

        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/sandrorat-apk-unknown/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/sandrorat-apk-unknown/SandroRat.apk')

        pkg_uuid = TestAssess.API_WRAPPER.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestAssess.PKG_LIST.append(pkg_uuid)

        tool = TestAssess.API_WRAPPER.getToolFromName('RevealDroid',
                                                      TestSwampApiWrapper.PROJECT)
        arun_uuid = TestAssess.API_WRAPPER.runAssessment(pkg_uuid,
                                                         tool.getIdentifierString(),
                                                         TestSwampApiWrapper.PROJECT,
                                                         None)
        self.assertIsNotNone(arun_uuid)

    def test_get_run_assess11(self):

        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/scarf-io-1.0/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/scarf-io-1.0/scarf-io.zip')

        pkg_uuid = TestAssess.API_WRAPPER.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestAssess.PKG_LIST.append(pkg_uuid)

        tool = TestAssess.API_WRAPPER.getToolFromName('OWASP Dependency Check',
                                                      TestSwampApiWrapper.PROJECT)
        arun_uuid = TestAssess.API_WRAPPER.runAssessment(pkg_uuid,
                                                         tool.getIdentifierString(),
                                                         TestSwampApiWrapper.PROJECT,
                                                         None)
        self.assertIsNotNone(arun_uuid)

    def test_get_run_assess12(self):
        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/sinatra-starter-2ad9cba672/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/sinatra-starter-2ad9cba672/sinatra-starter-2ad9cba672.zip')

        pkg_uuid = TestAssess.API_WRAPPER.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestAssess.PKG_LIST.append(pkg_uuid)

        tool = TestAssess.API_WRAPPER.getToolFromName('Dawn',
                                                      TestSwampApiWrapper.PROJECT)
        arun_uuid = TestAssess.API_WRAPPER.runAssessment(pkg_uuid,
                                                         tool.getIdentifierString(),
                                                         TestSwampApiWrapper.PROJECT,
                                                         None)
        self.assertIsNotNone(arun_uuid)

    def test_get_run_assess13(self):

        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/swamp-gradle-example-1.0/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/swamp-gradle-example-1.0/swamp-gradle-example-1.0.zip')

        pkg_uuid = TestAssess.API_WRAPPER.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestAssess.PKG_LIST.append(pkg_uuid)

        tool = TestAssess.API_WRAPPER.getToolFromName('checkstyle',
                                                      TestSwampApiWrapper.PROJECT)
        arun_uuid = TestAssess.API_WRAPPER.runAssessment(pkg_uuid,
                                                         tool.getIdentifierString(),
                                                         TestSwampApiWrapper.PROJECT,
                                                         None)
        self.assertIsNotNone(arun_uuid)

    def test_get_run_assess14(self):
        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/tomcat-coyote-7.0.27/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/tomcat-coyote-7.0.27/tomcat-coyote-7.0.27.tar.gz')

        pkg_uuid = TestAssess.API_WRAPPER.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestAssess.PKG_LIST.append(pkg_uuid)

        tool = TestAssess.API_WRAPPER.getToolFromName('Findbugs',
                                                      TestSwampApiWrapper.PROJECT)
        arun_uuid = TestAssess.API_WRAPPER.runAssessment(pkg_uuid,
                                                         tool.getIdentifierString(),
                                                         TestSwampApiWrapper.PROJECT,
                                                         None)
        self.assertIsNotNone(arun_uuid)

    def test_get_run_assess15(self):
        pkg_conf = osp.join(osp.dirname(__file__), 'resources/packages/wordpress-4.5.1/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__), 'resources/packages/wordpress-4.5.1/WordPress-4.5.1.zip')

        pkg_uuid = TestAssess.API_WRAPPER.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)
        TestAssess.PKG_LIST.append(pkg_uuid)

        tool = TestAssess.API_WRAPPER.getToolFromName('PHPMD',
                                                      TestSwampApiWrapper.PROJECT)
        arun_uuid = TestAssess.API_WRAPPER.runAssessment(pkg_uuid,
                                                         tool.getIdentifierString(),
                                                         TestSwampApiWrapper.PROJECT,
                                                         None)
        self.assertIsNotNone(arun_uuid)


class TestReporting(TestSwampApiWrapper):

    @classmethod
    def setUpClass(cls):
        super(TestReporting, cls).setUpClass()

        cls.API_WRAPPER = SwampApiWrapper()
        cls.API_WRAPPER.login(TestSwampApiWrapper.USERNAME,
                              TestSwampApiWrapper.PASSWORD,
                              TestSwampApiWrapper.HOST)

    @classmethod
    def tearDownClass(cls):
        cls.API_WRAPPER.logout()

    def test_get_results1(self):

        pkg_conf = osp.join(osp.dirname(__file__),
                            'resources/packages/swamp-gradle-example-1.0/package.conf')
        pkg_archive = osp.join(osp.dirname(__file__),
                               'resources/packages/swamp-gradle-example-1.0/swamp-gradle-example-1.0.zip')

        pkg_uuid = TestReporting.API_WRAPPER.uploadPackage(pkg_conf,
                                                        pkg_archive,
                                                        TestSwampApiWrapper.PROJECT,
                                                        True)
        self.assertIsNotNone(pkg_uuid)

        tool = TestReporting.API_WRAPPER.getToolFromName('Findbugs',
                                                      TestSwampApiWrapper.PROJECT)
        assessment_run = TestReporting.API_WRAPPER.runAssessment(pkg_uuid,
                                                                 tool.getIdentifierString(),
                                                                 TestSwampApiWrapper.PROJECT,
                                                                 None)
        self.assertIsNotNone(assessment_run)

        arun_results_uuid = None

        while True:
            assessment_record = TestReporting.API_WRAPPER.getAssessmentRecord(TestSwampApiWrapper.PROJECT,
                                                                               assessment_run.getUUIDString())
            status = AssessmentStatus.translateAssessmentStatus(assessment_record.getStatus())
            print(status, assessment_record.getStatus())
            time.sleep(10);
            if status == AssessmentStatus.FAILED or status == AssessmentStatus.SUCCESS:
                arun_results_uuid = assessment_record.getAssessmentResultUUID()
                break;

        outfile = osp.join(os.getcwd(), 'outfile.xml')
        TestReporting.API_WRAPPER.getAssessmentResults(TestSwampApiWrapper.PROJECT,
                                                       arun_results_uuid,
                                                       outfile)


if __name__ == '__main__':
    BasicConfigurator.configure(NullAppender())
    unittest.main()
