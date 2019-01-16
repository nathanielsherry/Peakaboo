./bin/linux/jpackager create-installer deb \
	-i ./input/ \
	-o ./output/deb/ \
	-n "Peakaboo" \
	-j Peakaboo.jar  \
	-c "peakaboo.ui.swing.Peakaboo" \
	--verbose  \
	--add-modules java.base,java.compiler,java.datatransfer,java.desktop,java.logging,java.naming,java.prefs,java.rmi,java.scripting,java.sql,jdk.jdi,jdk.unsupported \
	--version "$1" \
	--jvm-args "--illegal-access=permit" \
	--license-file "LICENSE" \
	--icon "icon-1024.png" \
	--linux-bundle-name "peakaboo"	
