// Separate debug-only entry point for rendering synthetic volume states on native surfaces.
// Production builds use lib/main.dart. This fixture never sends Bluetooth commands.
import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:dx340_phone/main.dart';
import 'package:dx340_phone/remote.dart';

Future<void> main() async {
  if (!kDebugMode) {
    throw StateError('Volume capture fixtures require a debug build.');
  }
  WidgetsFlutterBinding.ensureInitialized();
  String mode = 'volume-high';
  try {
    mode = textValue(await NativeBridge().call('captureTab'));
  } catch (_) {
    /* Default synthetic high boundary. */
  }
  runApp(
    DX340App(remote: Remote(VolumeCaptureBridge(low: mode == 'volume-low'))),
  );
  // Scroll only this fixture to the controls so captures show the new states.
  await Future<void>.delayed(const Duration(seconds: 2));
  bool moved = false;
  void scroll(Element element) {
    if (!moved &&
        element is StatefulElement &&
        element.state is ScrollableState) {
      final position = (element.state as ScrollableState).position;
      position.jumpTo(position.maxScrollExtent);
      moved = true;
    } else {
      element.visitChildren(scroll);
    }
  }

  WidgetsBinding.instance.rootElement?.visitChildren(scroll);
  await WidgetsBinding.instance.endOfFrame;
  void visit(Element element) {
    if (element.widget is Text &&
        (element.widget as Text).data == 'Volume up') {
      Scrollable.ensureVisible(element, alignment: 1);
    } else {
      element.visitChildren(visit);
    }
  }

  WidgetsBinding.instance.rootElement?.visitChildren(visit);
}

class VolumeCaptureBridge implements RemoteBridge {
  VolumeCaptureBridge({required this.low});
  final bool low;
  @override
  Stream<dynamic> get events => const Stream.empty();
  @override
  Future<dynamic> call(String method, [Json args = const {}]) async {
    if (method == 'captureTab' || method == 'resume') return null;
    if (method != 'snapshot') {
      throw StateError('Synthetic capture: Bluetooth actions are disabled.');
    }
    return {
      'bridgeVersion': 1,
      'status': 'Synthetic volume UI · no Bluetooth connection',
      'authorized': true,
      'mutationPending': false,
      'state': {
        'source': 'synthetic-mango',
        'sourceEpoch': 'fixture',
        'title': 'Volume presentation check',
        'artist': 'Synthetic fixture',
        'capabilities': {'canAdjustVolume': true},
        'volume': {
          'verified': true,
          'current': low ? 34 : 35,
          'verifiedMinimum': 34,
          'verifiedMaximum': 35,
          'profileId': 'synthetic-proof',
          'outputLabel': 'AMP15 phone out',
        },
      },
    };
  }
}
