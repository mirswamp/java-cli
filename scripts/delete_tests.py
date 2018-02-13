import os.path as osp
from confreader import read_conf_into_dict

from org.continuousassurance.swamp.cli import SwampApiWrapper


class Delete:

    def __init__(self, config_file):
        try:
            if config_file and osp.isfile(config_file):
                self.config_file = config_file
            else:
                self.config_file = osp.join(osp.dirname(__file__),
                                   'resources/userinfo.properties')

            self.user_conf = read_conf_into_dict(self.config_file)
        except IOError as err:
            print('''Please create "%s" with
            username=<swamp-username>
            password=<swamp-password>
            project=<test-project-uuid>
            hostname=<swamp-hostname>
            ''' % config_file)
            raise err

        self.api_wrapper = SwampApiWrapper()

    def login(self):
        self.api_wrapper.login(self.user_conf['username'],
                               self.user_conf['password'],
                               self.user_conf.get('hostname',
                                                  'https://www.mir-swamp.org'))

    def delete_pkgs(self):
        for pkg in self.api_wrapper.getPackagesList(self.user_conf['project']):
            self.api_wrapper.deletePackage(pkg)

    def delete_aruns(self):
        for arun in self.api_wrapper.getAllAssessments(self.user_conf['project']):
            self.api_wrapper.deleteAssessment(arun)

    def delete_results(self):
        for arun_record in self.api_wrapper.getAllAssessmentRecords(self.user_conf['project']):
            self.api_wrapper.deleteAssessmentRecord(arun_record)


if __name__ == '__main__':
    api_obj = Delete(None)
    api_obj.login()
    api_obj.delete_pkgs()
    api_obj.delete_aruns()
    api_obj.delete_results()
