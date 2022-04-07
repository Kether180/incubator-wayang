from typing import (Iterable, List)

from pywy.graph.graph import (GraphNode, WayangGraph)
from pywy.operators.base import PywyOperator


class NodeOperator(GraphNode[PywyOperator, PywyOperator]):

    def __init__(self, op: PywyOperator):
        super(NodeOperator, self).__init__(op)

    def get_adjacents(self) -> List[PywyOperator]:
        operator: PywyOperator = self.current
        if operator is None or operator.inputs == 0:
            return []
        return operator.inputOperator

    def build_node(self, t: PywyOperator) -> 'NodeOperator':
        return NodeOperator(t)


class WGraphOfOperator(WayangGraph[PywyOperator, NodeOperator]):

    def __init__(self, nodes: Iterable[PywyOperator]):
        super(WGraphOfOperator, self).__init__(nodes)

    def build_node(self, t: PywyOperator) -> NodeOperator:
        return NodeOperator(t)


class NodeVec(GraphNode[PywyOperator, List[PywyOperator]]):

    def __init__(self, op: PywyOperator):
        super(NodeVec, self).__init__([op, None])

    def get_adjacents(self) -> List[PywyOperator]:
        operator: PywyOperator = self.current[0]
        if operator is None or operator.inputs == 0:
            return []
        return operator.inputOperator

    def build_node(self, t: PywyOperator) -> 'NodeVec':
        return NodeVec(t)

    def __str__(self):
        return "NodeVec {}".format(self.current)

    def __repr__(self):
        return self.__str__()


class WGraphOfVec(WayangGraph[PywyOperator, NodeVec]):

    def __init__(self, nodes: Iterable[PywyOperator]):
        super(WGraphOfVec, self).__init__(nodes)

    def build_node(self, t: PywyOperator) -> NodeVec:
        return NodeVec(t)
