import importlib.util
from pathlib import Path
import unittest
spec = importlib.util.spec_from_file_location('observer', Path(__file__).parents[1] / 'observe-android-phone-background.py')
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)


class BackgroundTests(unittest.TestCase):
    def check(self, top='', wake='Awake', service='isForeground=true', pid='123'):
        activities = 'topResumedActivity=ActivityRecord{abc u0 ' + top + ' t21}' if top else ''
        return m.condition(service, activities, 'mWakefulness=' + wake, '123', pid)

    def test_actual_foreground_remote_rejected(self):
        self.assertFalse(self.check(m.PACKAGE + '/.MainActivity')['phoneAppBackground'])

    def test_settings_background_accepted(self):
        self.assertTrue(self.check('com.android.settings/.Settings')['phoneAppBackground'])

    def test_unknown_awake_activity_is_not_assumed_background(self):
        self.assertFalse(self.check()['phoneAppBackground'])

    def test_screen_off_without_resumed_activity(self):
        for wake in ['Asleep', 'Dozing']: self.assertTrue(self.check(wake=wake)['phoneAppBackground'])

    def test_missing_foreground_or_changed_process_not_accepted(self):
        self.assertFalse(self.check(service='startRequested=true')['phoneForegroundService'])
        self.assertFalse(self.check(pid='456')['phoneProcessUnchanged'])


if __name__ == '__main__': unittest.main()
