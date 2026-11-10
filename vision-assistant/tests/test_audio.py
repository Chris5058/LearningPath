import numpy as np

from iris.audio.listener import UtteranceSegmenter

RATE = 16000
CHUNK = 480  # 30 ms


def chunks(seconds, amplitude):
    rng = np.random.default_rng(0)
    n = int(seconds * RATE / CHUNK)
    return [(rng.standard_normal(CHUNK) * amplitude).astype(np.float32) for _ in range(n)]


def run(segmenter, stream):
    return [u for u in (segmenter.feed(c) for c in stream) if u is not None]


def test_cuts_one_utterance_out_of_silence():
    seg = UtteranceSegmenter(silence_seconds=0.6)
    out = run(seg, chunks(1, 0.001) + chunks(1.5, 0.2) + chunks(1, 0.001))
    assert len(out) == 1
    # speech + pre-roll + trailing silence
    assert 1.5 <= len(out[0]) / RATE <= 2.6


def test_silence_produces_nothing():
    assert run(UtteranceSegmenter(), chunks(3, 0.001)) == []


def test_short_click_is_dropped():
    seg = UtteranceSegmenter(silence_seconds=0.6)
    assert run(seg, chunks(1, 0.001) + chunks(0.09, 0.3) + chunks(1, 0.001)) == []


def test_two_utterances():
    seg = UtteranceSegmenter(silence_seconds=0.5)
    stream = chunks(0.5, 0.001) + chunks(1, 0.2) + chunks(1, 0.001) + chunks(1, 0.2) + chunks(1, 0.001)
    assert len(run(seg, stream)) == 2


def test_long_speech_is_cut_at_max_length():
    seg = UtteranceSegmenter(max_seconds=2)
    out = run(seg, chunks(5, 0.2))
    assert len(out) >= 2
