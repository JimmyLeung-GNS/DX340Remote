import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:dx340_phone/main.dart';
import 'package:dx340_phone/remote.dart';
import 'remote_test.dart' show snapshot;

class ReportBridge implements RemoteBridge {
  final calls = <String>[];
  var completion = Completer<dynamic>();
  @override
  Stream<dynamic> get events => const Stream.empty();
  @override
  Future<dynamic> call(String method, [Json args = const {}]) async {
    calls.add(method);
    if (method == 'snapshot') return snapshot(authorized: false);
    if (method == 'exportDiagnostics') return completion.future;
    return null;
  }
}

void main() {
  test(
    'export is explicit and single-flight without changing control state',
    () async {
      final bridge = ReportBridge();
      final remote = Remote(bridge)
        ..status = 'Existing connection status'
        ..error = 'Existing source error';
      expect(bridge.calls, isEmpty);
      final pending = remote.exportDiagnostics();
      await remote.exportDiagnostics();
      expect(bridge.calls, ['exportDiagnostics']);
      expect(remote.exportingDiagnostics, isTrue);
      bridge.completion.complete('Saved to Downloads/DX340Remote/report.json');
      await pending;
      expect(remote.exportingDiagnostics, isFalse);
      expect(remote.diagnosticsNotice, contains('Downloads'));
      expect(remote.status, 'Existing connection status');
      expect(remote.error, 'Existing source error');
      expect(remote.authorized, isFalse);
      remote.dispose();
    },
  );
  test(
    'empty native result never claims a saved report and failure can be retried',
    () async {
      final bridge = ReportBridge();
      final remote = Remote(bridge);
      final pending = remote.exportDiagnostics();
      bridge.completion.complete(null);
      await pending;
      expect(remote.diagnosticsNotice, contains('Could not prepare'));
      expect(remote.exportingDiagnostics, isFalse);
      bridge.completion = Completer<dynamic>();
      final retry = remote.exportDiagnostics();
      bridge.completion.completeError(
        PlatformException(
          code: 'REPORT_SAVE',
          message: 'Could not save the report.',
        ),
      );
      await retry;
      expect(bridge.calls, ['exportDiagnostics', 'exportDiagnostics']);
      expect(remote.diagnosticsNotice, 'Could not save the report.');
      expect(remote.exportingDiagnostics, isFalse);
      remote.dispose();
    },
  );
  test(
    'late export completion after disposal does not notify listeners',
    () async {
      final bridge = ReportBridge();
      final remote = Remote(bridge);
      var updates = 0;
      remote.addListener(() => updates++);
      final pending = remote.exportDiagnostics();
      expect(updates, 1);
      remote.dispose();
      bridge.completion.complete('Report ready');
      await pending;
      expect(updates, 1);
    },
  );
  for (final platform in [TargetPlatform.android, TargetPlatform.iOS]) {
    testWidgets(
      '$platform report is under connection help and sends no BLE command',
      (tester) async {
        debugDefaultTargetPlatformOverride = platform;
        addTearDown(() => debugDefaultTargetPlatformOverride = null);
        final bridge = ReportBridge();
        final remote = Remote(bridge);
        await tester.pumpWidget(DX340App(remote: remote));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Setup').last);
        await tester.pumpAndSettle();
        final label = platform == TargetPlatform.iOS
            ? 'Share connection report'
            : 'Save connection report';
        expect(find.text(label), findsNothing);
        await tester.tap(find.text('Connection help'));
        await tester.pumpAndSettle();
        await tester.scrollUntilVisible(find.text(label), 160);
        await tester.tap(find.text(label));
        await tester.pump();
        expect(
          bridge.calls.where((x) => x == 'exportDiagnostics'),
          hasLength(1),
        );
        expect(
          bridge.calls.any(
            (x) => [
              'send',
              'browse',
              'connectPlayer',
              'disconnect',
              'permissions',
            ].contains(x),
          ),
          isFalse,
        );
        bridge.completion.complete('Connection report ready');
        await tester.pumpAndSettle();
        expect(find.text('Connection report ready'), findsOneWidget);
        expect(tester.takeException(), isNull);
        await tester.pumpWidget(const SizedBox.shrink());
        debugDefaultTargetPlatformOverride = null;
      },
    );
  }
}
