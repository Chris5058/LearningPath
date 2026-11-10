from iris.wake import WakeGate


class Clock:
    def __init__(self):
        self.now = 0.0

    def __call__(self):
        return self.now


def gate(clock=None):
    return WakeGate(["hey iris", "iris"], followup_seconds=8, clock=clock or Clock())


def test_ignores_speech_without_wake_word():
    assert gate().filter("turn on the lamp") is None


def test_strips_wake_word():
    assert gate().filter("Hey Iris, what's the weather?") == "what's the weather?"
    assert gate().filter("iris turn on the lamp") == "turn on the lamp"


def test_wake_word_at_end():
    assert gate().filter("What time is it, Iris?") == "What time is it"


def test_bare_wake_word_returns_empty_command():
    assert gate().filter("Hey Iris.") == ""


def test_wake_word_must_be_a_whole_word():
    assert gate().filter("the iris of the eye is colourful") == "of the eye is colourful"
    assert gate().filter("that was satirist humour") is None


def test_followup_window():
    clock = Clock()
    g = gate(clock)
    g.open()
    clock.now = 5
    assert g.filter("and tomorrow?") == "and tomorrow?"
    clock.now = 9
    assert g.filter("and tomorrow?") is None


def test_close_ends_window():
    g = gate()
    g.open()
    g.close()
    assert g.filter("hello") is None
