#!/system/bin/sh
# Read-only, bounded diagnostic. No playback, Bluetooth, screen or power-setting action.
root=$1
duration=$2
interval=$3
unplugged_off=$4
status=FAILED
reason=UNEXPECTED_EXIT
logger=
count=0
start=
end=
umask 077
clock() { read seconds idle < /proc/uptime; printf '%s\n' "$seconds"; }
whole() { value=$(clock); printf '%s\n' "${value%%.*}"; }
write_state() { printf '%s\n' "$1" > "$root/state.tmp"; mv "$root/state.tmp" "$root/state"; }
logger_valid() {
    [ -n "$logger" ] && kill -0 "$logger" 2>/dev/null && [ -r "/proc/$logger/cmdline" ] || return 1
    command=$(tr '\000' ' ' < "/proc/$logger/cmdline")
    case "$command" in *logcat*) [ "$(readlink "/proc/$logger/fd/1")" = "$root/power-events.log" ];; *) return 1;; esac
}
finish() {
    end=$(clock)
    if logger_valid; then
        kill "$logger" 2>/dev/null; wait "$logger" 2>/dev/null
    elif [ "$status" = COMPLETED ]; then
        status=FAILED; reason=LOGGER_EXITED
    fi
    printf 'status\t%s\nreason\t%s\nstartedUptime\t%s\nendedUptime\t%s\nsamples\t%s\ndeadlineUptime\t%s\n' "$status" "$reason" "$start" "$end" "$count" "$deadline" > "$root/summary.tsv"
    write_state "$status"
}
trap finish EXIT
trap 'status=STOPPED; reason=OPERATOR_STOP; exit 2' TERM INT
printf '%s\n' "$$" > "$root/pid"
write_state ARMING
# stdout is flushed as records arrive; logcat's direct file stream can buffer
# records that would be lost on termination. A line cap bounds this stream and
# an early logger exit fails the observation rather than claiming full history.
logcat -b system -T 1 -m 1024 -v threadtime -v monotonic PowerManagerService:I PowerGroup:I DisplayDeviceRepository:I '*:S' > "$root/power-events.log" 2> "$root/logger.txt" &
logger=$!
printf '%s\n' "$logger" > "$root/logger-pid"
armed_deadline=$(($(whole) + 120))
dump() {
    if ! timeout 15 dumpsys "$2" > "$root/$1.txt" 2> "$root/error.txt"; then reason=DUMP_FAILED; exit 1; fi
}
display_state() {
    records=$(grep 'DisplayDeviceInfo{' "$1" | grep 'type INTERNAL' | grep 'FLAG_ALLOWED_TO_BE_DEFAULT_DISPLAY')
    number=$(printf '%s\n' "$records" | grep -c .)
    if [ "$number" != 1 ]; then printf 'Unknown\n'; return; fi
    printf '%s\n' "$records" | sed -n 's/.*, state \([A-Z_]*\),.*/\1/p'
}
streak=0
while :; do
    dump arming-power power
    dump arming-display display
    powered=$(sed -n 's/^  mIsPowered=//p' "$root/arming-power.txt")
    display=$(display_state "$root/arming-display.txt")
    if [ "$powered" = false ] && [ "$display" = OFF ]; then streak=$((streak + 1)); else streak=0; fi
    printf '%s\t%s\t%s\t%s\n' "$(clock)" "$powered" "$display" "$streak" >> "$root/arming.tsv"
    # Reject a brief charging-status fluctuation as an unplugged precondition.
    if [ "$unplugged_off" = 0 ] || [ "$streak" -ge 3 ]; then break; fi
    if [ "$(whole)" -ge "$armed_deadline" ]; then reason=ARMING_TIMEOUT; exit 1; fi
    sleep 5
done
if ! logger_valid; then reason=LOGGER_EXITED; exit 1; fi
host_pid=$(pidof org.dx340.remote.host)
if [ -z "$host_pid" ]; then reason=HOST_PROCESS_ABSENT; exit 1; fi
start=$(clock)
printf '%s\n' "$start" > "$root/started-uptime"
# Round the deadline up so fractional starting seconds cannot shorten a hold.
deadline=$((${start%%.*} + duration + 1))
last=${start%%.*}
write_state RUNNING
while :; do
    # A suspended process may next run after the observation deadline. Keep that
    # sampling gap visible; do not judge a later USB reconnection as an early wake.
    if [ "$count" -gt 0 ] && [ "$(whole)" -ge "$deadline" ]; then break; fi
    tag=$(printf '%03d' "$count")
    printf '%s\n' "$(clock)" > "$root/$tag-clock.txt"
    dump "$tag-power" power
    dump "$tag-display" display
    dump "$tag-media" media_session
    dump "$tag-audio" audio
    if ! timeout 15 dumpsys activity services org.dx340.remote.host > "$root/$tag-service.txt" 2> "$root/error.txt"; then reason=DUMP_FAILED; exit 1; fi
    pidof org.dx340.remote.host > "$root/$tag-pid.txt"
    display=$(display_state "$root/$tag-display.txt")
    powered=$(sed -n 's/^  mIsPowered=//p' "$root/$tag-power.txt")
    count=$((count + 1))
    if [ "$(cat "$root/$tag-pid.txt")" != "$host_pid" ]; then reason=HOST_PROCESS_CHANGED; exit 1; fi
    if [ "$unplugged_off" = 1 ] && { [ "$display" != OFF ] || [ "$powered" != false ]; }; then reason=UNPLUGGED_OFF_CONDITION_LOST; exit 1; fi
    if ! logger_valid; then reason=LOGGER_EXITED; exit 1; fi
    now=$(whole)
    if [ "$now" -lt "$last" ]; then reason=CLOCK_REGRESSION; exit 1; fi
    last=$now
    if [ "$now" -ge "$deadline" ]; then break; fi
    if [ "$count" -ge 256 ]; then reason=SAMPLE_BOUND; exit 1; fi
    remaining=$((deadline - now))
    delay=$interval
    if [ "$remaining" -lt "$delay" ]; then delay=$remaining; fi
    sleep "$delay"
done
status=COMPLETED
reason=DURATION_REACHED
