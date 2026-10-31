// Developer-only simulator entrypoint. Ordinary main.dart never imports this file.
// Uses the real native bridge/SDK, with no source, Bluetooth or audio fixture.
import 'dart:convert';
import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:dx340_phone/main.dart';
import 'package:dx340_phone/remote.dart';

void main() {
  if (!kDebugMode || !Platform.isIOS) {
    throw UnsupportedError(
      'Report probe requires a debug iOS Simulator build.',
    );
  }
  WidgetsFlutterBinding.ensureInitialized();
  final bridge = NativeBridge();
  final remote = Remote(bridge);
  runApp(DX340App(remote: remote));
  WidgetsBinding.instance.addPostFrameCallback((_) async {
    await Future<void>.delayed(const Duration(seconds: 2));
    final token = await bridge.call('captureTab');
    if (token is! String ||
        !RegExp(r'^report-[A-Fa-f0-9-]{36}$').hasMatch(token)) {
      throw StateError('Missing unique report-probe token');
    }
    final marker = File('${Directory.systemTemp.path}/dx340-$token.json');
    try {
      final value = await bridge.call('diagnostics');
      final data = jsonDecode(value as String) as Map<String, dynamic>;
      await remote.exportDiagnostics();
      final notice = remote.diagnosticsNotice ?? '';
      await marker.writeAsString(
        jsonEncode({
          'token': token,
          'modelInstanceId': data['modelInstanceId'],
          'status':
              notice == 'Choose where to save or share the connection report.'
              ? 'presented'
              : 'failed',
          'notice': notice,
        }),
      );
    } catch (error) {
      await marker.writeAsString(
        jsonEncode({
          'token': token,
          'status': 'failed',
          'error': error.toString(),
        }),
      );
    }
  });
}
