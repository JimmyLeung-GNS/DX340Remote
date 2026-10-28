import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:dx340_phone/main.dart';
import 'package:dx340_phone/remote.dart';
import 'remote_test.dart' show FakeBridge;
import '../test_support/volume_capture.dart' show VolumeCaptureBridge;

void main() {
  for (final platform in [TargetPlatform.android, TargetPlatform.iOS]) {
    testWidgets(
      '$platform volume boundaries use the shared enabled states at large text',
      (tester) async {
        debugDefaultTargetPlatformOverride = platform;
        addTearDown(() => debugDefaultTargetPlatformOverride = null);
        tester.platformDispatcher.textScaleFactorTestValue = 2.5;
        addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
        await tester.pumpWidget(
          DX340App(remote: Remote(VolumeCaptureBridge(low: false))),
        );
        await tester.pumpAndSettle();
        await tester.scrollUntilVisible(
          find.text('Volume up'),
          200,
          scrollable: find.byType(Scrollable).first,
        );
        expect(tester.takeException(), isNull);
        if (platform == TargetPlatform.android) {
          expect(
            tester
                .widget<OutlinedButton>(
                  find.widgetWithText(OutlinedButton, 'Volume down'),
                )
                .onPressed,
            isNotNull,
          );
          expect(
            tester
                .widget<OutlinedButton>(
                  find.widgetWithText(OutlinedButton, 'Volume up'),
                )
                .onPressed,
            isNull,
          );
        } else {
          expect(
            tester
                .widget<CupertinoButton>(
                  find.widgetWithText(CupertinoButton, 'Volume down'),
                )
                .onPressed,
            isNotNull,
          );
          expect(
            tester
                .widget<CupertinoButton>(
                  find.widgetWithText(CupertinoButton, 'Volume up'),
                )
                .onPressed,
            isNull,
          );
        }
        await tester.pumpWidget(const SizedBox());
        debugDefaultTargetPlatformOverride = null;
      },
    );
    testWidgets(
      '$platform disconnected library has working Setup recovery at large text',
      (tester) async {
        debugDefaultTargetPlatformOverride = platform;
        addTearDown(() => debugDefaultTargetPlatformOverride = null);
        await tester.pumpWidget(DX340App(remote: Remote(FakeBridge())));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Library').last);
        await tester.pumpAndSettle();
        expect(find.text('Open Setup'), findsOneWidget);
        await tester.tap(find.text('Open Setup'));
        await tester.pumpAndSettle();
        expect(find.text('Find DX340'), findsOneWidget);
        tester.platformDispatcher.textScaleFactorTestValue = 2.5;
        addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
        await tester.tap(find.text('Player').last);
        await tester.pumpAndSettle();
        await tester.scrollUntilVisible(
          find.text('Play'),
          200,
          scrollable: find.byType(Scrollable).first,
        );
        expect(tester.takeException(), isNull);
        if (platform == TargetPlatform.iOS) {
          expect(
            MediaQuery.textScalerOf(
              tester.element(find.text('Library').last),
            ).scale(10),
            10,
            reason:
                'Fixed-height iOS tabs follow Cupertino native sizing at accessibility text sizes.',
          );
          expect(
            MediaQuery.textScalerOf(
              tester.element(find.text('No track connected')),
            ).scale(10),
            greaterThan(10),
            reason:
                'Reading content must still respect the user’s larger text setting.',
          );
        }
        if (platform == TargetPlatform.android) {
          expect(
            tester
                .widget<OutlinedButton>(
                  find.widgetWithText(OutlinedButton, 'Play'),
                )
                .onPressed,
            isNull,
          );
        } else {
          expect(
            tester
                .widget<CupertinoButton>(
                  find.widgetWithText(CupertinoButton, 'Play'),
                )
                .onPressed,
            isNull,
          );
        }
        await tester.pumpWidget(const SizedBox());
        debugDefaultTargetPlatformOverride = null;
      },
    );
  }
}
