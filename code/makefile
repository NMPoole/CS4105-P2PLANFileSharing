
JFLAGS = -g
JC = javac

.SUFFIXES: .java .class

.java.class:
	@ $(JC) $(JFLAGS) $*.java

CLASSES = \
    BeaconReceiver.java \
    BeaconSender.java \
	ByteReader.java \
	Configuration.java \
	DeleteReceiver.java \
	DownloadReceiver.java \
	FileTreeBrowser.java \
	LogFileWriter.java \
	Message.java \
	MulticastHandler.java \
	SearchReceiver.java \
	TCPBrowseServer.java \
	TCPBrowseServerThread.java \
	TCPClient.java \
	TCPFileServer.java \
	UploadReceiver.java



default: clean classes

classes: $(CLASSES:.java=.class)

clean:
	@$(RM) *.class

