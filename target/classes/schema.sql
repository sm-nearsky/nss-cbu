CREATE TABLE IF NOT EXISTS BackupFileDataPackets (
    fileUpdateID INT,
    totalBytes INT,
    packetNumber INT,
    packetsTotal INT,
    dateTimeCaptured TIMESTAMP,
    fileAction INT,
    fileData VARCHAR(255),
    PRIMARY KEY (fileUpdateID)
);
