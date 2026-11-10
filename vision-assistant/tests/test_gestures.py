from iris.vision.gestures import GestureStabilizer, SwipeDetector


def feed(stabilizer, labels):
    return [stabilizer.update(label) for label in labels]


def test_pose_fires_once_after_hold():
    s = GestureStabilizer(hold_frames=3)
    assert feed(s, ["fist"] * 6) == [None, None, "fist", None, None, None]


def test_flicker_does_not_fire():
    s = GestureStabilizer(hold_frames=3)
    assert feed(s, ["fist", "fist", None, "fist", "fist"]) == [None] * 5


def test_pose_rearms_after_hand_leaves():
    s = GestureStabilizer(hold_frames=2)
    out = feed(s, ["open_palm", "open_palm", None, "open_palm", "open_palm"])
    assert out == [None, "open_palm", None, None, "open_palm"]


def test_changing_pose_fires_new_pose():
    s = GestureStabilizer(hold_frames=2)
    assert feed(s, ["fist", "fist", "thumbs_up", "thumbs_up"]) == [None, "fist", None, "thumbs_up"]


def swipe(xs, dt=0.1, detector=None, start=0.0):
    detector = detector or SwipeDetector()
    return [detector.update(x, start + i * dt) for i, x in enumerate(xs)]


def test_swipe_named_from_users_point_of_view():
    # Hand moves toward the left of the image = the user's right.
    assert "swipe_right" in swipe([0.8, 0.65, 0.5, 0.35, 0.2])
    assert "swipe_left" in swipe([0.2, 0.35, 0.5, 0.65, 0.8])


def test_slow_movement_is_not_a_swipe():
    assert swipe([0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8], dt=0.4) == [None] * 7


def test_jitter_is_not_a_swipe():
    assert swipe([0.5, 0.52, 0.49, 0.51, 0.5, 0.52]) == [None] * 6


def test_swipe_fires_once_then_cools_down():
    d = SwipeDetector(cooldown=1.0)
    first = swipe([0.2, 0.35, 0.5, 0.65, 0.8], detector=d)
    again = swipe([0.2, 0.35, 0.5, 0.65, 0.8], detector=d, start=0.5)
    assert first.count("swipe_left") == 1
    assert again == [None] * 5


def test_losing_the_hand_resets_tracking():
    assert swipe([0.2, 0.35, None, 0.65, 0.8]) == [None] * 5
