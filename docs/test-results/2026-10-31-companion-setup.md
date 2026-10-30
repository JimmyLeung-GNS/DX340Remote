# Companion connection and approval setup

31 October 2026, Hong Kong. UX-01 IN PROGRESS; full project ACTIVE/incomplete. This pass preserves the native Android system appearance and the owner preference to keep optional Mango browsing enabled.

## Change and intended behavior

The previous main screen placed controller access after local tests, source toggles and permission actions; its controller panel placed saved identities/revoke actions before a pending approval. The new order puts a short connection summary, explicit Start/Stop and pairing-window action, then controller access before secondary setup controls. A new request is shown before the collapsed saved-controller list. The panel distinguishes unpaired, pairing-window-closed, awaiting approval, approved/retry and request-ended states without inventing an expiry cause. Approve retains the existing host-epoch, matching pending identity, OS bond, window and capacity checks. No approvals, bonds or controller ownership rules change.

Permissions/access, optional browsing/local checks and advanced diagnostics have separate disclosures. Existing operations remain available. Permission grants, Bluetooth on/off, notification-listener access and bound optional services are reported separately. Playback access uses Android's public [NotificationManager.isNotificationListenerAccessGranted](https://developer.android.com/reference/android/app/NotificationManager#isNotificationListenerAccessGranted(android.content.ComponentName)) for this app's own declared component; it is a grant check, not a source playback proof. No settings are changed by a refresh. Display padding now uses dp, text remains scalable sp and native headings/buttons retain accessibility semantics and minimum48dp actions.

The debug controller preview uses isolated preferences, synthetic identities and a null live-service provider. Its paired/unpaired/closed/approved/ended screens are presentation fixtures; an enabled synthetic Approve cannot authorize a real controller. They do not prove an actual OS pairing/approval or physical security boundary.

## Current checks and package scope

**Verified:** Android assembly/lint and existing164 JVM tests (protocol46/cache7/adapters111) pass on the current Windows/JBR21/SDK36 environment. The final layout/permission-gating rebuild passed assembly/lint with unchanged protocol tests retained. No fake layout-mirroring unit tests were added.

Candidate companion SHA256 **49eb883bcba01601f7fdda0acdaeb16b2993efd4ea734633c996926a8775cfde**, original signer **24f48aa3566dcf7b65774ee12585191a8a0df9eeff9cc49e9e72c22bacca6e8d** verified against the installed app before upgrade. Installed baseline is58314b84… until the physical follow-up below; Pixel ordinary ea9f165d… remains unchanged. Existing audible selections stay scoped to their recorded packages.

**Pending:** in-place DX340 upgrade/render review, preference/source/output preservation and saved-grant phone read reconnect on this companion. No new playback/audio or live approval/revocation test has run. Permission-denied/Bluetooth-off runtime and full TalkBack coverage are also not verified by compilation.

Private preflight, packages/build logs and later captures are under ignored `artifacts/companion-setup-20261031/`. The preflight stores all four host preference files, source sessions/output and Accessibility state. No signing material is copied into Git.

## Apple follow-up from the previous application checkpoint

Native job [41119813702](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/41119813702) has completed SUCCESS at31047c22. Artifact13007024569 SHA256 **bcf53639c6fd3f9fbf9d8203c021e37b96a1b7bd33d9d536b426de50e2da4854** was downloaded, hash-verified and safely extracted; inspected logs show **14 Swift tests, zero failures**, actual native build success and simulator captures. This is simulator evidence, not owner-iPhone signing/BLE/audio. The current Companion UI changes do not edit Apple/shared-phone code. Shared job41119813675 is still in progress; retain its handle and inspect the terminal result without restarting an active job.
