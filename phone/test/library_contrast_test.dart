import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:dx340_phone/main.dart';
import 'package:dx340_phone/remote.dart';
import 'remote_test.dart' show FakeBridge, snapshot;

void main() {
  for (final brightness in Brightness.values) {
    for (final highContrast in [false, true]) {
      testWidgets(
        'iPhone Library toolbar contrast $brightness highContrast=$highContrast is scoped',
        (tester) async {
          debugDefaultTargetPlatformOverride = TargetPlatform.iOS;
          addTearDown(() => debugDefaultTargetPlatformOverride = null);
          tester.platformDispatcher.platformBrightnessTestValue = brightness;
          tester.platformDispatcher.accessibilityFeaturesTestValue =
              FakeAccessibilityFeatures(highContrast: highContrast);
          addTearDown(
            tester.platformDispatcher.clearPlatformBrightnessTestValue,
          );
          addTearDown(
            tester.platformDispatcher.clearAccessibilityFeaturesTestValue,
          );
          final bridge = FakeBridge();
          final remote = Remote(bridge);
          await tester.pumpWidget(DX340App(remote: remote));
          await tester.pumpAndSettle();
          remote.accept(
            snapshot(
              library: {
                'revision': 'fixture',
                'items': [
                  {'id': 'folder', 'title': 'Album', 'browsable': true},
                ],
              },
            ),
          );
          await tester.tap(find.text('Library').last);
          await tester.pumpAndSettle();
          final action = CupertinoTheme.of(
            tester.element(find.text('Browse source library')),
          );
          final row = CupertinoTheme.of(tester.element(find.text('Album')));
          final low = action.primaryColor.computeLuminance();
          final high = action.scaffoldBackgroundColor.computeLuminance();
          expect(
            low > high
                ? (low + .05) / (high + .05)
                : (high + .05) / (low + .05),
            greaterThanOrEqualTo(4.5),
          );
          await tester.tap(find.text('Queue').last);
          await tester.pumpAndSettle();
          expect(
            CupertinoTheme.of(
              tester.element(find.text('Refresh queue')),
            ).primaryColor,
            row.primaryColor,
            reason: 'Toolbar tint must not change neighboring destinations',
          );
          await tester.pumpWidget(const SizedBox());
          debugDefaultTargetPlatformOverride = null;
        },
      );
    }
  }
}
