#!/bin/bash
# Center Client: automatic start-up test.
#
# Starts the desktop version of the game (it runs the same game and Center
# Client code as the browser version) on a virtual screen, clicks through the
# first screens, and takes pictures along the way. The test fails if the game
# crashes or never finishes loading.
#
# The pictures are saved in smoke/shots (uploaded as the "smoke-test" artifact)
# and also printed into the log as text, between @@BEGIN name@@ and @@END name@@.

set -u
ROOT="$(pwd)"
OUT="$ROOT/smoke"
SHOTS="$OUT/shots"
LOG="$OUT/game.log"
mkdir -p "$OUT/classes" "$SHOTS"

fail() {
	echo "SMOKE TEST FAILED: $1"
	echo "----- last 200 lines of the game log -----"
	tail -n 200 "$LOG" 2>/dev/null
	[ -n "${GAME:-}" ] && kill "$GAME" 2>/dev/null
	exit 1
}

# ---- 1. Compile the desktop version ------------------------------------------
DEPS="$(ls "$ROOT"/desktopRuntime/eclipseProject/deps_fix/*.jar | tr '\n' ':')"
find src/main/java src/game/java src/protocol-game/java src/lwjgl/java src/centerclient/java -name '*.java' > "$OUT/sources"
echo "Compiling $(wc -l < "$OUT/sources") files..."
javac -J-Xmx4g -nowarn -encoding UTF-8 --release 17 -proc:none -cp "$DEPS" -d "$OUT/classes" @"$OUT/sources" 2>&1 \
	| grep -v '^Note:' || true
[ -f "$OUT/classes/net/centerclient/CenterClient.class" ] || fail "the code didn't compile"

# ---- 2. Start a virtual screen and the game -----------------------------------
# The game makes its window 200 x 250 smaller than the screen, so this gives a 1366 x 768 window.
Xvfb :99 -screen 0 1566x1018x24 +extension GLX +render -noreset -nolisten tcp >/dev/null 2>&1 &
export DISPLAY=:99
sleep 2

RT="UnsafeMemcpy.jar:lwjgl.jar:lwjgl-egl.jar:lwjgl-glfw.jar:lwjgl-jemalloc.jar:lwjgl-openal.jar:lwjgl-opengles.jar"
RT="$RT:soundsystem-20120107.jar:codecjorbis-20101023.jar:codecwav-20101023.jar"
RT="$RT:Java-WebSocket-1.5.1-with-dependencies.jar:webrtc-java-0.8.0.jar"

# start_game <graphics> <use bundled ANGLE: yes/no>
start_game() {
	cd "$ROOT/desktopRuntime"
	if [ "$2" = "yes" ]; then
		LD_LIBRARY_PATH="$ROOT/desktopRuntime" java -Xmx2G $JDEBUG -Djava.library.path=. -cp "$OUT/classes:$RT" \
			net.lax1dude.eaglercraft.internal.MainClass hide-renderdoc $1 > "$LOG" 2>&1 &
	else
		java -Xmx2G $JDEBUG -Djava.library.path=. -cp "$OUT/classes:$RT" \
			net.lax1dude.eaglercraft.internal.MainClass hide-renderdoc $1 > "$LOG" 2>&1 &
	fi
	GAME=$!
	cd "$ROOT"
}

# Start with the HUD modules turned on, so they get tested in the world later.
# (The desktop version keeps saved data in files named _eagstorage.<key>.dat.)
cat > "$ROOT/desktopRuntime/_eagstorage.centerclient.profile.Default.dat" <<'JSON'
{"version":1,"modules":{
"Watermark":{"enabled":true},"FPS":{"enabled":true},"Ping":{"enabled":true},"Speed":{"enabled":true},
"CPS Counter":{"enabled":true},"Coordinates":{"enabled":true},"Clock":{"enabled":true},"Server Info":{"enabled":true},
"Armor Status":{"enabled":true},"Potion Effects":{"enabled":true},"Keystrokes":{"enabled":true}}}
JSON

# What the virtual screen offers (for working out graphics problems).
echo "----- graphics on the virtual screen -----"
eglinfo -B 2>&1 | grep -iE "platform|version|renderer|error" | head -n 30
glxinfo -B 2>&1 | grep -iE "version|renderer|error" | head -n 10
# No speakers on the test computer: give OpenAL a silent sound device so the game doesn't wait 30 seconds for sound.
export ALSOFT_DRIVERS=null
JDEBUG=""

# A virtual screen has no graphics card, so try a few software graphics setups until one works.
STARTED=no
for setup in "opengl yes" "vulkan yes" "default yes" "default no"; do
	set -- $setup
	ARG="$1"; [ "$ARG" = "default" ] && ARG=""
	echo "Trying graphics: ${1}, bundled ANGLE: $2"
	start_game "$ARG" "$2"
	for i in $(seq 1 20); do
		sleep 1
		if grep -q "OpenGL Renderer" "$LOG"; then STARTED=yes; break; fi
		kill -0 "$GAME" 2>/dev/null || break
	done
	if [ "$STARTED" = yes ]; then
		grep -E "OpenGL Version|OpenGL Renderer|ANGLE Platform" "$LOG"
		break
	fi
	grep -vE "^\s+at " "$LOG" | grep -iE "error|exception|fail|libEGL|libGLES|egl|glfw|loaded|warn" | head -n 40
	kill "$GAME" 2>/dev/null
	sleep 1
done
[ "$STARTED" = yes ] || fail "no graphics setup worked on the virtual screen"

# ---- Helpers -----------------------------------------------------------------
WID=""
find_window() {
	[ -n "$WID" ] && return 0
	# no window manager runs on the virtual screen, so the game's window is the only visible one
	WID="$(xdotool search --onlyvisible --name "." 2>/dev/null | tail -n 1)"
	[ -n "$WID" ] && echo "Game window: $WID \"$(xdotool getwindowname "$WID" 2>/dev/null)\""
	[ -n "$WID" ]
}

shot() { # shot <name> [thumbnail]
	local file="$SHOTS/$1.png"
	if find_window; then
		import -window "$WID" "$file" 2>/dev/null
	else
		import -window root "$file" 2>/dev/null
	fi
	[ -f "$file" ] || return 0
	local small="$OUT/print.jpg"
	if [ "${2:-}" = "thumbnail" ]; then
		convert "$file" -resize 480x -quality 60 "$small"
	else
		convert "$file" -quality 82 "$small"
	fi
	echo "@@BEGIN $1@@"
	base64 -w 4000 "$small"
	echo "@@END $1@@"
}

alive() { kill -0 "$GAME" 2>/dev/null; }

crashed() { grep -qE "Crash Report|Game Crashed|Exception in thread \"main\"" "$LOG"; }

wait_for_log() { # wait_for_log <text> <seconds>
	local i=0
	while [ "$i" -lt "$2" ]; do
		grep -qF "$1" "$LOG" && return 0
		alive || fail "the game closed while waiting for: $1"
		crashed && fail "the game crashed while waiting for: $1"
		sleep 1
		i=$((i + 1))
	done
	fail "timed out waiting for: $1"
}

# Minecraft's automatic GUI scale for the current window size.
gui_scale() {
	local w h f
	eval "$(xdotool getwindowgeometry --shell "$WID")"
	w=$WIDTH; h=$HEIGHT; f=1
	while [ $((w / (f + 1))) -ge 320 ] && [ $((h / (f + 1))) -ge 240 ]; do f=$((f + 1)); done
	echo "$f $w $h"
}

click_gui() { # click_gui <gui x> <gui y>   (Minecraft GUI coordinates)
	read -r s w h <<< "$(gui_scale)"
	xdotool mousemove --window "$WID" $(($1 * s + s / 2)) $(($2 * s + s / 2)) click 1
}

key() { # key <xdotool key name>
	xdotool windowfocus --sync "$WID" 2>/dev/null
	xdotool key --clearmodifiers "$1"
}

screen_count() { grep -c "Screen: $1" "$LOG"; }

# ---- 3. The loading screen ---------------------------------------------------
wait_for_log "Initializing Hooks" 90
find_window
n=0
while [ "$n" -lt 30 ] && ! grep -q "Screen: " "$LOG"; do
	alive || fail "the game closed while loading"
	crashed && fail "the game crashed while loading"
	n=$((n + 1))
	shot "loading_$(printf %02d $n)" thumbnail
	sleep 0.4
done
wait_for_log "Screen: GuiScreenEditProfile" 150
find_window || fail "couldn't find the game window"
sleep 3
shot "edit_profile"

# ---- 4. Click through to the Center Client title screen ----------------------
read -r S W H <<< "$(gui_scale)"
GW=$((W / S)); GH=$((H / S))
echo "Window ${W}x${H}, GUI scale $S, GUI size ${GW}x${GH}"

wait_for_screen() { # wait_for_screen <name> <count before> <seconds>
	local i=0
	while [ "$i" -lt "$3" ]; do
		[ "$(screen_count "$1")" -gt "$2" ] && return 0
		alive || fail "the game closed while waiting for screen $1"
		crashed && fail "the game crashed while waiting for screen $1"
		sleep 1
		i=$((i + 1))
	done
	fail "timed out waiting for screen $1"
}

click_gui $((GW / 2)) $((GH / 6 + 168 + 10)) # "Done" on the profile screen
sleep 2
if grep -q "Screen: GuiScreenDefaultUsernameNote" "$LOG"; then
	click_gui $((GW / 2)) $((GH / 6 + 142 + 10)) # "Continue anyway"
	sleep 2
fi
if grep -q "Screen: GuiScreenFirstLoad" "$LOG"; then
	shot "first_load_notice" thumbnail
	BEFORE=$(screen_count CenterMainMenu)
	click_gui $((GW / 2)) $((GH / 2 + 30 + 10)) # "I Understand"
	wait_for_screen CenterMainMenu "$BEFORE" 20
fi
wait_for_log "Screen: CenterMainMenu" 20
sleep 3 # let the buttons slide in
shot "main_menu"

# ---- 5. The Right Shift menu on the title screen -----------------------------
BEFORE=$(screen_count ClickGui)
key Shift_R
wait_for_screen ClickGui "$BEFORE" 10
sleep 1.5
shot "click_gui_menu"
key Escape
sleep 1.5

# ---- 6. Make a world and test the HUD in game --------------------------------
# Center Client title screen: the Singleplayer button
LOGO_Y=$(( (GH - 161) / 2 - 10 )); [ "$LOGO_Y" -lt 10 ] && LOGO_Y=10
ROW1=$((LOGO_Y + 75 + 14 + 22 + 6))
click_gui $((GW / 2 - 100 + 49)) $((ROW1 + 10))
sleep 2
if grep -q "Screen: GuiWorldSelection" "$LOG"; then
	shot "world_list" thumbnail
	click_gui $((GW / 2 + 4 + 75)) $((GH - 52 + 10)) # "Create New World"
	sleep 2
fi
if grep -q "Screen: GuiScreenCreateWorldSelection" "$LOG"; then
	click_gui $((GW / 2)) $((GH / 4 + 40 + 10)) # Eaglercraft's "Create New World"
	sleep 2
fi
wait_for_log "Screen: GuiCreateWorld" 10
click_gui $((GW / 2 - 155 + 75)) $((GH - 28 + 10)) # "Create New World"
echo "Making a world..."
wait_for_log "Screen: none (playing)" 180
sleep 6 # let the chunks appear
shot "in_game_hud"

# Hold W: the Keystrokes W key should light up
xdotool windowfocus --sync "$WID" 2>/dev/null
xdotool keydown w
sleep 0.6
shot "keystrokes_w_held"
xdotool keyup w
sleep 0.5

# Click fast: the CPS numbers should go up
xdotool click --repeat 7 --delay 90 1
shot "cps_after_clicks"
sleep 1.5

# The Right Shift menu in game
BEFORE=$(screen_count ClickGui)
key Shift_R
wait_for_screen ClickGui "$BEFORE" 10
sleep 1.5
shot "click_gui_in_game"
key Escape
sleep 1

# ---- 6. Finish ---------------------------------------------------------------
crashed && fail "the game crashed"
alive || fail "the game closed"
echo "----- Center Client lines in the game log -----"
grep -iE "center client|Screen: |ERROR|Exception" "$LOG" | head -n 80
kill "$GAME" 2>/dev/null
echo "SMOKE TEST PASSED"
