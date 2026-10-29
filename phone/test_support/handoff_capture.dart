// Separate debug presentation target. It cannot send Bluetooth commands.
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
  runApp(DX340App(remote: Remote(HandoffCaptureBridge(mode))));
}

class HandoffCaptureBridge implements RemoteBridge {
  HandoffCaptureBridge(this.mode);
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
    final lost = mode.contains('lost');
    final pending = mode.contains('pending');
    return {
      'bridgeVersion': 1,
      'status': lost
          ? 'Synthetic UI: control moved to another approved controller. Music continues on DX340.'
          : pending
          ? 'Synthetic UI: requesting control on this phone…'
          : 'Synthetic UI: another approved controller has control. Open Setup to take control on this phone.',
      'authorized': false,
      'controlRequestAvailable': !lost && !pending,
      'state': {},
    };
  }
}
