package com.github.tth05.minecraftnbtintellijplugin.editor.ui;

import com.github.tth05.minecraftnbtintellijplugin.NBTTagTreeNode;
import com.github.tth05.minecraftnbtintellijplugin.NBTTagType;
import com.github.tth05.minecraftnbtintellijplugin.util.NBTFileUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.ui.treeStructure.Tree;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

public class NBTFileEditorUI extends JPanel implements DataProvider {

	public static final DataKey<NBTFileEditorUI> DATA_KEY = DataKey.create(NBTFileEditorUI.class.getName());

	private Tree tree;

	private boolean autoSaveEnabled = true;

	private boolean littleEndian;
	private boolean network;
	private boolean levelDat;
	private final MutableInt levelDatVersion = new MutableInt();

	public NBTFileEditorUI(@NotNull VirtualFile file, @NotNull Project project) {
		this.setLayout(new BorderLayout());

		//Toolbar
		JPanel northSection = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JButton loadButton = new JButton("Load", AllIcons.Actions.MenuSaveall);
		loadButton.addMouseListener(new MouseAdapter() {
			JBLabel errorText;

			@Override
			public void mouseReleased(MouseEvent e) {
				JBLabel errorText = NBTFileEditorUI.this.load(file, project, northSection);
				if (this.errorText != null)
					NBTFileEditorUI.this.remove(this.errorText);
				this.errorText = errorText;
				if (errorText == null)
					northSection.remove(loadButton);
			}
		});

		JBCheckBox leCheckbox = new JBCheckBox("Little Endian");
		leCheckbox.addItemListener(e -> littleEndian = e.getStateChange() == ItemEvent.SELECTED);

		JBCheckBox networkCheckbox = new JBCheckBox("Network");
		networkCheckbox.addItemListener(e -> network = e.getStateChange() == ItemEvent.SELECTED);

		JBCheckBox levelDatCheckbox = new JBCheckBox("level.dat");
		levelDatCheckbox.addItemListener(e -> levelDat = e.getStateChange() == ItemEvent.SELECTED);

		northSection.add(loadButton);
		northSection.add(leCheckbox);
		northSection.add(networkCheckbox);
		northSection.add(levelDatCheckbox);

		this.add(northSection, BorderLayout.NORTH);
	}

	private JBLabel load(@NotNull VirtualFile file, @NotNull Project project, @NotNull JPanel northSection) {
		levelDatVersion.setValue(0);
		//Tree Section
		DefaultMutableTreeNode root = NBTFileUtil.loadNBTFileIntoTree(file, this.littleEndian, this.network, this.levelDat ? this.levelDatVersion : null);
		if (root == null) {
			levelDatVersion.setValue(0);
			JBLabel errorText = new JBLabel("Invalid NBT File!");
			errorText.setForeground(JBColor.RED);
			errorText.setHorizontalAlignment(SwingConstants.CENTER);
			this.add(errorText, BorderLayout.CENTER);
			return errorText;
		}

		TreeModel model = new DefaultTreeModel(root);
		//The listener updates the indices in the node names if their parent is some sort of list
		model.addTreeModelListener(new TreeModelListener() {
			@Override
			public void treeNodesChanged(TreeModelEvent e) {
			}

			@Override
			public void treeNodesInserted(TreeModelEvent e) {
			}

			@Override
			public void treeNodesRemoved(TreeModelEvent e) {
				NBTTagTreeNode parent = (NBTTagTreeNode) e.getTreePath().getLastPathComponent();

				if (parent.getChildCount() > 0 && parent.getType() != NBTTagType.COMPOUND) {
					Enumeration<TreeNode> children = parent.children();
					for (int i = 0; children.hasMoreElements(); i++)
						((NBTTagTreeNode) children.nextElement()).setName(i + "");
				}
			}

			@Override
			public void treeStructureChanged(TreeModelEvent e) {
			}
		});

		this.tree = new Tree(model);
		this.tree.setCellRenderer(new NBTFileEditorTreeCellRenderer());
		this.tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					//Find right clicked row and make sure it's selected
					int row = tree.getClosestRowForLocation(e.getX(), e.getY());

					if (!tree.isRowSelected(row))
						tree.setSelectionRow(row);

					ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.POPUP,
							(ActionGroup) ActionManager.getInstance().getAction(
									"com.github.tth05.minecraftnbtintellijplugin.actions.NBTFileEditorPopupGroup"));
					menu.setTargetComponent(NBTFileEditorUI.this);
					menu.getComponent().show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});

		this.add(new JBScrollPane(this.tree), BorderLayout.CENTER);

		//Toolbar
		JButton saveButton = new JButton("Save", AllIcons.Actions.MenuSaveall);
		saveButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				NBTFileUtil.saveTreeToFile(NBTFileEditorUI.this.tree, file, project, littleEndian, network, levelDat ? levelDatVersion.getValue() : null);
			}
		});

		JBCheckBox autoSaveCheckbox = new JBCheckBox("Save On Change");
		autoSaveCheckbox.setSelected(true);
		autoSaveCheckbox.addItemListener(e -> autoSaveEnabled = e.getStateChange() == ItemEvent.SELECTED);

		if (levelDat) {
			IntegerField levelDatVersionField = new IntegerField("level.dat version", 0, Integer.MAX_VALUE);
			levelDatVersionField.setToolTipText("level.dat version");
			levelDatVersionField.setValue(levelDatVersion.getValue());
			levelDatVersionField.setEditable(false);
			northSection.add(levelDatVersionField);
		}

		northSection.add(saveButton);
		northSection.add(autoSaveCheckbox);

		return null;
	}

	public Tree getTree() {
		return this.tree;
	}

	@Nullable
	@Override
	public Object getData(@NotNull String dataId) {
		if (DATA_KEY.is(dataId))
			return this;
		return null;
	}

	public boolean isAutoSaveEnabled() {
		return autoSaveEnabled;
	}

	public boolean isLittleEndian() {
		return littleEndian;
	}

	public boolean isNetwork() {
		return network;
	}

	public boolean isLevelDat() {
		return levelDat;
	}

	public MutableInt getLevelDatVersion() {
		return levelDatVersion;
	}
}
