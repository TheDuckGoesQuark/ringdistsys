CREATE TABLE node(
    address VARCHAR(255) NOT NULL,
    port INT NOT NULL,
    nodeId INT NOT NULL,
    successorId INT NULL,
    PRIMARY KEY (nodeId),
    FOREIGN KEY (successorId) REFERENCES node(nodeId)
);

CREATE TABLE coordinator(
    coordinatorId INT,
    PRIMARY KEY (coordinatorId),
    FOREIGN KEY (coordinatorId) REFERENCES node(nodeId)
    ON DELETE SET NULL
)