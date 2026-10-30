import importlib.util
from pathlib import Path
import plistlib
import sys
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('mac_iphone', Path(__file__).with_name('mac-iphone-tests.py'))
mac = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mac)


class PhysicalBuildGuards(unittest.TestCase):
    def app(self, root, platform='iphoneos', bundle=mac.BUNDLE):
        app = root / 'Runner.app'
        app.mkdir()
        with (app / 'Info.plist').open('wb') as file:
            plistlib.dump({'CFBundleIdentifier': bundle, 'DTPlatformName': platform,
                          'CFBundleSupportedPlatforms': ['iPhoneOS' if platform == 'iphoneos' else 'iPhoneSimulator']}, file)
        return app

    def test_simulator_arm64_app_is_not_a_physical_install(self):
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaisesRegex(ValueError, 'simulator app'):
                mac.validate_device_app(self.app(Path(temp), 'iphonesimulator'), mac.BUNDLE)

    def test_wrong_app_identity_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaisesRegex(ValueError, 'identifier differs'):
                mac.validate_device_app(self.app(Path(temp), bundle='org.example.other'), mac.BUNDLE)

    def test_capture_target_cannot_become_the_physical_app(self):
        with tempfile.TemporaryDirectory() as temp:
            phone = Path(temp)
            config = phone / 'Generated.xcconfig'
            config.write_text('FLUTTER_TARGET=test_support/handoff_capture.dart\n')
            with self.assertRaisesRegex(ValueError, 'synthetic Flutter target'):
                mac.ordinary_target(config, phone)
            config.write_text('FLUTTER_TARGET=lib/main.dart\n')
            mac.ordinary_target(config, phone)

    def test_missing_device_metadata_is_unknown_not_a_confirmed_os(self):
        items = mac.device_inventory({'result': {'devices': [{'identifier': 'id', 'hardwareProperties': {'udid': 'real-udid'}}]}})
        self.assertEqual(items[0]['udid'], 'real-udid')
        self.assertIsNone(items[0]['osVersion'])
        self.assertIsNone(items[0]['developerMode'])

    def test_real_command_deadline_terminates_the_test_child(self):
        with tempfile.TemporaryDirectory() as temp:
            session = mac.Session('test', Path(temp))
            with self.assertRaisesRegex(RuntimeError, 'command deadline'):
                session.call('bounded-child', [sys.executable, '-c', 'import time; time.sleep(20)'], timeout=0.1)
            self.assertTrue(session.report['lastTimedOutCommand']['processTerminated'])


if __name__ == '__main__':
    unittest.main()
