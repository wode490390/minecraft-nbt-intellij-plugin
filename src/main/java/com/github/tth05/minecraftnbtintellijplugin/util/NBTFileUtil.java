package com.github.tth05.minecraftnbtintellijplugin.util;

import com.github.tth05.minecraftnbtintellijplugin.NBTTagTreeNode;
import com.github.tth05.minecraftnbtintellijplugin.NBTTagType;
import com.github.tth05.minecraftnbtintellijplugin.editor.ui.NBTFileEditorUI;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.Tree;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipException;

// https://minecraft.gamepedia.com/NBT_format
public class NBTFileUtil {

	/**
	 * Uses the event to get the current project and file and then calls {@link #saveTreeToFile(Tree, VirtualFile, Project, boolean, boolean, Integer)}
	 * This method is only used for auto-saving and only called by actions
	 *
	 * @param event The event
	 */
	public static void saveTree(AnActionEvent event) {
		NBTFileEditorUI nbtFileEditorUI = event.getData(NBTFileEditorUI.DATA_KEY);
		Project project = event.getProject();
		VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);

		if (nbtFileEditorUI == null || nbtFileEditorUI.getTree() == null || file == null) {
			new Notification("NBTSaveError",
					"Error saving NBT file",
					"Due to an unknown error the file could not be saved.",
					NotificationType.WARNING).notify(project);
			return;
		}

		if (!nbtFileEditorUI.isAutoSaveEnabled())
			return;

		saveTreeToFile(nbtFileEditorUI.getTree(), file, project, nbtFileEditorUI.isLittleEndian(), nbtFileEditorUI.isNetwork(), nbtFileEditorUI.isLevelDat() ? nbtFileEditorUI.getLevelDatVersion().getValue() : null);
	}

	/**
	 * Serializes the given tree and writes the bytes to the file. If the saving failed, a notification will be shown.
	 *
	 * @param tree    The tree to be serialized
	 * @param file    The file to write the bytes to
	 * @param project The current project to show the notification in
	 */
	public static void saveTreeToFile(Tree tree, VirtualFile file, Project project, boolean littleEndian, boolean network, @Nullable Integer levelDatVersion) {
		if (levelDatVersion != null) {
			littleEndian = true;
			network = false;
		} else if (network) {
			littleEndian = true;
		}
		boolean le = littleEndian;
		boolean net = network;

		ApplicationManager.getApplication().runWriteAction(() -> {
			DataOutput outputStream = null;
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				if (net)
					outputStream = new NetworkDataOutputStream(baos);
				else if (le)
					outputStream = new LittleEndianDataOutputStream(baos);
				else
					outputStream = new DataOutputStream(new GZIPOutputStream(baos));

				writeNodeToStream((NBTTagTreeNode) tree.getModel().getRoot(), outputStream, true);

				try (OutputStream os = file.getOutputStream(tree)) {
					if (levelDatVersion != null) {
						LittleEndianDataOutputStream ledos = new LittleEndianDataOutputStream(os);
						ledos.writeInt(levelDatVersion);
						ledos.writeInt(baos.size());
					}

					os.write(baos.toByteArray());
				}
			} catch (IOException ex) {
				new Notification("NBTSaveError",
						"Error saving NBT file",
						"Due to an unknown error the file could not be saved: " + ex.getMessage(),
						NotificationType.WARNING).notify(project);
			} finally {
				if (outputStream != null)
					try {
						((Closeable) outputStream).close();
					} catch (IOException ignored) {
					}
			}
		});
	}

	private static void writeNodeToStream(NBTTagTreeNode node,
	                                      DataOutput stream,
	                                      boolean writeName) throws IOException {
		if (writeName) {
			stream.writeByte(node.getType().getId());
			stream.writeUTF(node.getName());
		}
		switch (node.getType()) {
			case BYTE:
				stream.writeByte((Byte) node.getValue());
				break;
			case SHORT:
				stream.writeShort((Short) node.getValue());
				break;
			case LONG:
				stream.writeLong((Long) node.getValue());
				break;
			case INT:
				stream.writeInt((Integer) node.getValue());
				break;
			case FLOAT:
				stream.writeFloat((Float) node.getValue());
				break;
			case DOUBLE:
				stream.writeDouble((Double) node.getValue());
				break;
			case BYTE_ARRAY:
				stream.writeInt(node.getChildCount());
				Enumeration<TreeNode> byteArrayChildren = node.children();
				while (byteArrayChildren.hasMoreElements()) {
					NBTTagTreeNode child = (NBTTagTreeNode) byteArrayChildren.nextElement();
					stream.writeByte((Byte) (child).getValue());
				}
				break;
			case STRING:
				stream.writeUTF((String) node.getValue());
				break;
			case LIST:
				if (node.getChildCount() > 0)
					stream.writeByte(((NBTTagTreeNode) node.getFirstChild()).getType().getId());
				else
					stream.writeByte(0);
				stream.writeInt(node.getChildCount());
				Enumeration<TreeNode> listChildren = node.children();
				while (listChildren.hasMoreElements())
					writeNodeToStream((NBTTagTreeNode) listChildren.nextElement(), stream, false);
				break;
			case COMPOUND:
				Enumeration<TreeNode> compoundChildren = node.children();
				while (compoundChildren.hasMoreElements())
					writeNodeToStream((NBTTagTreeNode) compoundChildren.nextElement(), stream, true);
				stream.writeByte(0);
				break;
			case INT_ARRAY:
				stream.writeInt(node.getChildCount());
				Enumeration<TreeNode> intArrayChildren = node.children();
				while (intArrayChildren.hasMoreElements())
					stream.writeInt((Integer) ((NBTTagTreeNode) intArrayChildren.nextElement()).getValue());
				break;
			case LONG_ARRAY:
				stream.writeInt(node.getChildCount());
				Enumeration<TreeNode> longArrayChildren = node.children();
				while (longArrayChildren.hasMoreElements())
					stream.writeLong((Long) ((NBTTagTreeNode) longArrayChildren.nextElement()).getValue());
				break;
		}
	}

	@Nullable
	public static DefaultMutableTreeNode loadNBTFileIntoTree(VirtualFile file, boolean littleEndian, boolean network, @Nullable MutableInt levelDatVersion) {
		if (levelDatVersion != null) {
			littleEndian = true;
			network = false;
		} else if (network) {
			littleEndian = true;
		}

		DataInput data = null;
		try {
			if (network)
				data = new NetworkDataInputStream(file.getInputStream());
			else if (littleEndian)
				data = new LittleEndianDataInputStream(file.getInputStream());
			else
				data = uncompress(file.getInputStream());

			if (levelDatVersion != null) {
				levelDatVersion.setValue(data.readInt());
				int length = data.readInt();
			}

			//Get tag id
			int type = data.readUnsignedByte();
			String name = data.readUTF(); // Root tag name

			switch (type) {
				case 10:
					NBTTagTreeNode root = new NBTTagTreeNode(NBTTagType.COMPOUND, name, null);
					loadNBTDataOfCompound(root, data);
					return root;
				case 9:
					int listType = data.readUnsignedByte();
					int listSize = data.readInt();
					NBTTagTreeNode listNode = new NBTTagTreeNode(NBTTagType.LIST, "", listSize + " elements");
					for (int i = 0; i < listSize; i++)
						listNode.add(createNode(listType, "[" + i + "]", data));
					return listNode;
			}

			return null;
		} catch (IOException e) {
			return null;
		} finally {
			if (data != null)
				try {
					((Closeable) data).close();
				} catch (IOException ignored) {
				}
		}
	}

	private static void loadNBTDataOfCompound(DefaultMutableTreeNode root, DataInput data) throws IOException {
		while (true) {
			//Get tag id
			int type = data.readUnsignedByte();

			if (type != 0)
				root.add(createNode(type, data.readUTF(), data));
			else
				return;
		}
	}

	private static NBTTagTreeNode createNode(int type, String name, DataInput data) throws IOException {
		switch (type) {
			case 1:
				return new NBTTagTreeNode(NBTTagType.BYTE, name, data.readByte());
			case 2:
				return new NBTTagTreeNode(NBTTagType.SHORT, name, data.readShort());
			case 3:
				return new NBTTagTreeNode(NBTTagType.INT, name, data.readInt());
			case 4:
				return new NBTTagTreeNode(NBTTagType.LONG, name, data.readLong());
			case 5:
				return new NBTTagTreeNode(NBTTagType.FLOAT, name, data.readFloat());
			case 6:
				return new NBTTagTreeNode(NBTTagType.DOUBLE, name, data.readDouble());
			case 7:
				int byteArraySize = data.readInt();
				NBTTagTreeNode byteArrayNode = new NBTTagTreeNode(NBTTagType.BYTE_ARRAY, name, byteArraySize + " elements");
				for (int i = 0; i < byteArraySize; i++)
					byteArrayNode.add(createNode(1, "[" + i + "]", data));
				return byteArrayNode;
			case 8:
				return new NBTTagTreeNode(NBTTagType.STRING, name, data.readUTF());
			case 9:
				int listType = data.readUnsignedByte();
				int listSize = data.readInt();
				NBTTagTreeNode listNode = new NBTTagTreeNode(NBTTagType.LIST, name, listSize + " elements");
				for (int i = 0; i < listSize; i++)
					listNode.add(createNode(listType, "[" + i + "]", data));
				return listNode;
			case 10:
				NBTTagTreeNode compoundNode = new NBTTagTreeNode(NBTTagType.COMPOUND, name, null);
				loadNBTDataOfCompound(compoundNode, data);
				return compoundNode;
			case 11:
				int intArraySize = data.readInt();
				NBTTagTreeNode intArrayNode = new NBTTagTreeNode(NBTTagType.INT_ARRAY, name, intArraySize + " elements");
				for (int i = 0; i < intArraySize; i++)
					intArrayNode.add(createNode(3, "[" + i + "]", data));
				return intArrayNode;
			case 12:
				int longArraySize = data.readInt();
				NBTTagTreeNode longArrayNode = new NBTTagTreeNode(NBTTagType.LONG_ARRAY, name, longArraySize + " elements");
				for (int i = 0; i < longArraySize; i++)
					longArrayNode.add(createNode(4, "[" + i + "]", data));
				return longArrayNode;
			default:
				throw new IOException("Unknown tag id found: " + type);
		}
	}

	private static DataInputStream uncompress(InputStream input) throws IOException {
		try {
			final GZIPInputStream inputGzipStream = new GZIPInputStream(input);
			return new DataInputStream(inputGzipStream);
		} catch (ZipException e) {
			//Data is not compressed
			input.reset();
			return new DataInputStream(input);
		}
	}
}
