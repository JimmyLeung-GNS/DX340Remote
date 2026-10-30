"""Filesystem/Git provenance guards only; no Apple signing or iPhone simulation."""
import argparse
import importlib.util
import json
from pathlib import Path
import plistlib
import subprocess
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('mac_iphone_receipts', Path(__file__).with_name('mac-iphone-tests.py'))
mac = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mac)


class BuildReceiptTests(unittest.TestCase):
    def test_cold_flutter_bootstrap_finishes_before_strict_machine_json(self):
        with tempfile.TemporaryDirectory() as temp:
            session = mac.Session('test', Path(temp))
            bootstrapped = False
            def call(label, *args, **kwargs):
                nonlocal bootstrapped
                if label == 'flutter-bootstrap':
                    bootstrapped = True
                    return 0, 'download progress, then ordinary version output'
                if label == 'flutter-version':
                    if not bootstrapped:
                        return 0, 'download progress\n{}'
                    return 0, json.dumps({'frameworkVersion': mac.FLUTTER_VERSION,
                                         'frameworkRevision': mac.FLUTTER_REVISION})
                if label == 'simulators':
                    return 0, '{"devices": {}}'
                if label == 'source-commit':
                    return 0, 'a' * 40
                return 0, ''
            session.call = call
            with patch.object(mac.platform, 'system', return_value='Darwin'), patch.object(mac.shutil, 'which', return_value='/fixture/flutter'):
                self.assertTrue(session.inventory())
            self.assertTrue(bootstrapped)
            self.assertEqual(mac.FLUTTER_REVISION, session.report['flutter']['frameworkRevision'])

    def test_unreadable_bundle_directory_cannot_be_omitted_from_the_receipt(self):
        with tempfile.TemporaryDirectory() as temp:
            app = self.app(Path(temp))
            def denied(*args, **kwargs):
                kwargs['onerror'](PermissionError('directory cannot be inspected'))
            with patch.object(mac.os, 'walk', side_effect=denied):
                with self.assertRaises(PermissionError):
                    mac.app_contents_receipt(app)

    def app(self, root):
        app = root / 'Runner.app'
        app.mkdir(parents=True)
        with (app / 'Info.plist').open('wb') as file:
            plistlib.dump({'CFBundleIdentifier': mac.BUNDLE, 'DTPlatformName': 'iphoneos',
                          'CFBundleSupportedPlatforms': ['iPhoneOS']}, file)
        (app / 'Runner').write_bytes(b'synthetic executable, not a signed app')
        return app

    def source(self, root):
        tree = root / 'source'
        tree.mkdir()
        def git(*args):
            return subprocess.check_output(['git', '-C', str(tree), *args], stderr=subprocess.DEVNULL)
        git('init')
        (tree / 'tracked.dart').write_text('original source', encoding='utf-8')
        git('add', 'tracked.dart')
        git('-c', 'user.name=DX340 Test', '-c', 'user.email=test@example.invalid', 'commit', '-m', 'fixture')
        return tree, git('rev-parse', 'HEAD').decode().strip()

    def test_bundle_digest_is_path_independent_and_covers_empty_directories(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            first, second = self.app(root / 'a'), self.app(root / 'b')
            self.assertEqual(mac.app_contents_receipt(first), mac.app_contents_receipt(second))
            (second / 'empty').mkdir()
            self.assertNotEqual(mac.app_contents_receipt(first), mac.app_contents_receipt(second))

    def test_mutated_added_and_removed_resources_change_the_recorded_app(self):
        with tempfile.TemporaryDirectory() as temp:
            app = self.app(Path(temp))
            before = mac.app_contents_receipt(app)
            (app / 'Runner').write_bytes(b'different executable, same identity')
            self.assertNotEqual(before, mac.app_contents_receipt(app))
            changed = mac.app_contents_receipt(app)
            (app / 'new-resource').write_bytes(b'added')
            self.assertNotEqual(changed, mac.app_contents_receipt(app))
            (app / 'new-resource').unlink()
            self.assertEqual(changed, mac.app_contents_receipt(app))
            (app / 'Runner').unlink()
            self.assertNotEqual(before, mac.app_contents_receipt(app))

    def link(self, target, path):
        try:
            path.symlink_to(target)
        except OSError as error:
            self.skipTest('OS symlink creation unavailable: ' + str(error))

    def test_internal_links_are_recorded_and_retargeting_changes_the_receipt(self):
        with tempfile.TemporaryDirectory() as temp:
            app = self.app(Path(temp))
            (app / 'second').write_bytes((app / 'Runner').read_bytes())
            self.link('Runner', app / 'link')
            before = mac.app_contents_receipt(app)
            (app / 'link').unlink()
            self.link('second', app / 'link')
            self.assertNotEqual(before, mac.app_contents_receipt(app))

    def test_external_or_broken_symlink_cannot_be_sealed_as_a_complete_app(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            app = self.app(root)
            (root / 'outside').write_bytes(b'external')
            self.link('../outside', app / 'link')
            with self.assertRaisesRegex(ValueError, 'escapes'):
                mac.app_contents_receipt(app)
            (app / 'link').unlink()
            self.link('missing', app / 'link')
            with self.assertRaisesRegex(ValueError, 'Broken'):
                mac.app_contents_receipt(app)

    def test_source_receipt_binds_worktree_commit_and_post_build_edits(self):
        with tempfile.TemporaryDirectory() as temp:
            tree, commit = self.source(Path(temp))
            before = mac.worktree_receipt(tree, commit)
            self.assertFalse(before['hasSourceChanges'])
            (tree / 'tracked.dart').write_text('changed source', encoding='utf-8')
            self.assertNotEqual(before, mac.worktree_receipt(tree, commit))
            with self.assertRaisesRegex(ValueError, 'worktree/commit changed'):
                mac.worktree_receipt(tree, '0' * 40)

    def test_receipt_requires_current_record_and_rejects_changed_app_or_source(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            tree, commit = self.source(root)
            app = self.app(root)
            prior = {'sourceCommit': commit}
            with self.assertRaisesRegex(ValueError, 'no app/source binding'):
                mac.verify_install_receipt(app, tree, prior)
            prior.update(buildWorktree=mac.worktree_receipt(tree, commit), signedAppContents=mac.app_contents_receipt(app))
            mac.verify_install_receipt(app, tree, prior)
            (app / 'Runner').write_bytes(b'changed, even if resigned by same team')
            with self.assertRaisesRegex(ValueError, 'app contents changed'):
                mac.verify_install_receipt(app, tree, prior)
            (tree / 'tracked.dart').write_text('later edit', encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'worktree changed after signing'):
                mac.verify_install_receipt(app, tree, prior)

    def test_install_dispatch_refuses_changed_artifact_before_any_device_command(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            tree, commit = self.source(root)
            config = tree / 'phone/ios/Flutter/Generated.xcconfig'
            config.parent.mkdir(parents=True)
            config.write_text('FLUTTER_TARGET=lib/main.dart\n', encoding='utf-8')
            build = root / 'build'
            app = self.app(build)
            prior = {'status': 'SIGNED_ORDINARY_DEVICE_BUILD_PASSED', 'requestedDevice': 'device',
                     'sourceCommit': commit, 'sourceWorktree': str(tree), 'appPath': str(app),
                     'bundleId': mac.BUNDLE, 'team': 'ABCDEFGHIJ',
                     'buildWorktree': mac.worktree_receipt(tree, commit),
                     'signedAppContents': mac.app_contents_receipt(app)}
            (build / 'summary.json').write_text(json.dumps(prior), encoding='utf-8')
            (app / 'Runner').write_bytes(b'changed signed output')
            session = mac.Session('test', root / 'evidence')
            session.inventory = lambda: session.report.update(sourceCommit=commit) or True
            commands = []
            session.call = lambda label, *args, **kwargs: commands.append(label) or (0, '')
            with patch.object(mac, 'Session', return_value=session):
                self.assertEqual(1, mac.run(argparse.Namespace(mode='device-install', device='device', run=str(build))))
            self.assertEqual([], commands)
            self.assertIn('app contents changed', session.report['error'])


if __name__ == '__main__':
    unittest.main()
