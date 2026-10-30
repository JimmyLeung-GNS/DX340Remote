// Debug-only presentation fixture. It has no Bluetooth command path.
import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:dx340_phone/main.dart';
import 'package:dx340_phone/remote.dart';

Future<void> main() async {
  if (!kDebugMode) {
    throw StateError('Synthetic captures require a debug build.');
  }
  WidgetsFlutterBinding.ensureInitialized();
  final mode = textValue(await NativeBridge().call('captureTab'));
  runApp(DX340App(remote: Remote(SetupCaptureBridge(mode))));
}

class SetupCaptureBridge implements RemoteBridge {
  SetupCaptureBridge(this.mode);
  final String mode;
  @override
  Stream<dynamic> get events => const Stream.empty();
  @override
  Future<dynamic> call(String method, [Json args = const {}]) async {
    if (method == 'captureTab') return 'setup';
    if (method == 'resume') return null;
    if (method != 'snapshot') {
      throw StateError('Synthetic capture: Bluetooth actions disabled.');
    }
    final phase = mode.replaceFirst('setup-', '');
    return {
      'bridgeVersion': 1,
      'status':
          'Synthetic presentation only; no physical Bluetooth connection.',
      'authorized': phase == 'connected',
      'connectionSetup': {
        'phase': phase,
        'hasSavedPlayer': phase == 'connected',
        'savedName': 'Fixture player',
      },
      'devices': phase == 'choosing'
          ? [
              {'id': 'fixture-a', 'name': 'Fixture player A'},
              {'id': 'fixture-b', 'name': 'Fixture player B'},
            ]
          : [],
      'state': {},
    };
  }
}
