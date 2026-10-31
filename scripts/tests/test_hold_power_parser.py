"""Run the actual recorder awk function; no Android/configuration operations."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).parents[1] / 'device-hold-recorder.sh'


class HoldPowerParserTests(unittest.TestCase):
    def parse(self, content):
        shell = shutil.which('sh')
        if not shell:
            git_shell = Path('C:/Program Files/Git/usr/bin/sh.exe')
            shell = str(git_shell) if git_shell.exists() else None
        if not shell:
            self.skipTest('POSIX shell required; Android parser execution is a separate check')
        text = SCRIPT.read_text(encoding='utf-8')
        function = text[text.index('powered_state() {'):text.index('\nstreak=0')]
        with tempfile.TemporaryDirectory() as directory:
            fixture = Path(directory) / 'power.txt'
            fixture.write_text(content, encoding='utf-8', newline='\n')
            return subprocess.check_output([shell, '-c', function + '\npowered_state "$1"',
                                            'parser-test', fixture.as_posix()], timeout=10).decode().strip()

    def test_main_state_isolated_from_repeated_battery_saver_value(self):
        for main in ('true', 'false'):
            for saver in ('true', 'false'):
                with self.subTest(main=main, saver=saver):
                    text = f'POWER MANAGER (dumpsys power)\n\nPower Manager State:\n  mWakefulness=Awake\n  mIsPowered={main}\n  mPlugType=2\n\nBattery saver state:\n  mIsPowered={saver}\n'
                    self.assertEqual(self.parse(text), main)

    def test_missing_main_or_value_never_borrows_a_subsection(self):
        for text in ('', 'Battery saver state:\n  mIsPowered=false\n',
                     'Power Manager State:\n  mWakefulness=Asleep\nBattery saver state:\n  mIsPowered=false\n'):
            with self.subTest(text=text): self.assertEqual(self.parse(text), 'Unknown')

    def test_duplicate_invalid_or_repeated_main_sections_fail_closed(self):
        for text in ('Power Manager State:\n  mIsPowered=false\n  mIsPowered=false\n',
                     'Power Manager State:\n  mIsPowered=False\n',
                     'Power Manager State:\n  mIsPowered=unknown\n',
                     'Power Manager State:\n  mIsPowered=false\nBattery saver state:\n  mIsPowered=true\nPower Manager State:\n  mIsPowered=false\n'):
            with self.subTest(text=text): self.assertEqual(self.parse(text), 'Unknown')


if __name__ == '__main__': unittest.main()
