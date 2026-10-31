import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:dx340_phone/main.dart';
import 'package:dx340_phone/remote.dart';
import 'remote_test.dart' show FakeBridge;
import 'library_stability_test.dart' show librarySnapshot;

void main() {
  for (final platform in [TargetPlatform.android, TargetPlatform.iOS]) {
    for (final scale in [1.0, 2.5]) {
      testWidgets(
        '$platform scale $scale fresh browsing stays reachable after a scrolled page expires',
        (tester) async {
          debugDefaultTargetPlatformOverride = platform;
          addTearDown(() => debugDefaultTargetPlatformOverride = null);
          tester.view.physicalSize = const Size(400, 800);
          tester.view.devicePixelRatio = 1;
          tester.platformDispatcher.textScaleFactorTestValue = scale;
          addTearDown(tester.view.resetPhysicalSize);
          addTearDown(tester.view.resetDevicePixelRatio);
          addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
          final bridge = FakeBridge();
          final remote = Remote(bridge);
          await tester.pumpWidget(DX340App(remote: remote));
          await tester.pumpAndSettle();
          final page = librarySnapshot();
          (page['state'] as Json)['capabilities'] = {
            'canBrowse': true,
            'canBrowseFolders': true,
            'canPlayItem': true,
          };
          (page['library'] as Json)['uiAssisted'] = true;
          remote.accept(page);
          await tester.tap(find.text('Library').last);
          await tester.pumpAndSettle();
          bridge.calls.clear();
          final browse = find.text('Browse source library');
          final toolbarPosition = tester.getTopLeft(browse);
          final list = tester.state<ScrollableState>(
            find.descendant(
              of: find.byType(ListView),
              matching: find.byType(Scrollable),
            ),
          );
          list.position.jumpTo(list.position.maxScrollExtent);
          await tester.pumpAndSettle();
          expect(browse.hitTestable(), findsOneWidget);
          expect(tester.getTopLeft(browse), toolbarPosition);
          final row = find.text('Song 8');
          expect(row.hitTestable(), findsOneWidget);
          final rowPosition = tester.getTopLeft(row);
          final finger = await tester.startGesture(tester.getCenter(row));
          (page['library'] as Json)['invalidated'] = true;
          remote.accept(page);
          await tester.pump();
          expect(tester.getTopLeft(row), rowPosition);
          expect(tester.getTopLeft(browse), toolbarPosition);
          await finger.up();
          await tester.pumpAndSettle();
          expect(bridge.calls, isEmpty);
          await tester.tap(browse);
          await tester.pumpAndSettle();
          expect(bridge.calls.single.$1, 'browse');
          expect(bridge.calls.single.$2, isEmpty);
          // Loading and a pending mutation must not permit a second request.
          final loading = librarySnapshot(pending: true);
          (loading['library'] as Json)['loading'] = true;
          remote.accept(loading);
          await tester.pump();
          await tester.tap(browse);
          await tester.pump();
          expect(bridge.calls.length, 1);
          final lost = librarySnapshot()..['authorized'] = false;
          (lost['library'] as Json)['invalidated'] = true;
          remote.accept(lost);
          await tester.pumpAndSettle();
          await tester.tap(browse);
          await tester.pumpAndSettle();
          expect(bridge.calls.length, 1);
          expect(find.text(remote.libraryRecovery), findsNothing);
          expect(tester.takeException(), isNull);
          await tester.pumpWidget(const SizedBox());
          debugDefaultTargetPlatformOverride = null;
        },
      );
    }
  }
}
