"""A stand-in for the Anthropic client that replays scripted responses."""

from types import SimpleNamespace


def text(value):
    return SimpleNamespace(type="text", text=value)


def tool_use(name, args, id="tu_1"):
    return SimpleNamespace(type="tool_use", name=name, input=args, id=id)


def response(*content, stop_reason="end_turn"):
    return SimpleNamespace(content=list(content), stop_reason=stop_reason)


class FakeClient:
    def __init__(self, *responses):
        self._responses = list(responses)
        self.requests = []
        self.beta = SimpleNamespace(messages=SimpleNamespace(create=self._create))

    def _create(self, **kwargs):
        # Snapshot the messages: the caller keeps appending to the same list.
        self.requests.append({**kwargs, "messages": list(kwargs["messages"])})
        item = self._responses.pop(0)
        if isinstance(item, Exception):
            raise item
        return item
