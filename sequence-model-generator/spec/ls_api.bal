const SEQUENCE_DIAGRAM_API = "sequenceModelGeneratorService";

const GET_SEQUENCE_MODEL_API = "getSequenceDiagramModel";

type GetSequenceModelApi record {|
    GET_SEQUENCE_MODEL_API api = GET_SEQUENCE_MODEL_API;
    record {|
        string filePath;
        LinePosition 'start;
        LinePosition end;
    |} request;
    record {|
        SequenceDiagram sequenceDiagram;
    |} response;
|};
