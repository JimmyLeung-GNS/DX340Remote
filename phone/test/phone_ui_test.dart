import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:dx340_phone/main.dart';
import 'package:dx340_phone/remote.dart';
import 'remote_test.dart' show FakeBridge;

void main() {
  for (final platform in [TargetPlatform.android, TargetPlatform.iOS]) {
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
      await tester.scrollUntilVisible(find.text('Play'), 200, scrollable: find.byType(Scrollable).first);
        expect(tester.takeException(), isNull);
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
