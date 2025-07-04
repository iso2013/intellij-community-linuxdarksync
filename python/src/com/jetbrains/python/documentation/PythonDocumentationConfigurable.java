// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.python.documentation;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.PlatformUtils;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.jetbrains.python.PyBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;


public final class PythonDocumentationConfigurable implements SearchableConfigurable, Configurable.NoScroll {
  private final PythonDocumentationPanel myPanel = new PythonDocumentationPanel();

  @Override
  public @NotNull String getId() {
    return PythonDocumentationProvider.DOCUMENTATION_CONFIGURABLE_ID;
  }

  @Override
  public @Nls String getDisplayName() {
    return PlatformUtils.isPyCharm() ? PyBundle.message("external.documentation.pycharm")
                                     : PyBundle.message("external.documentation.python.plugin");
  }

  @Override
  public String getHelpTopic() {
    return "preferences.ExternalDocumentation";
  }

  @Override
  public JComponent createComponent() {
    Border border = IdeBorderFactory.createEmptyBorder(JBUI.insets(10, 0));
    JPanel panelWithDescription = JBUI.Panels.simplePanel(new JBLabel(PyBundle.message("external.documentation.description"))).withBorder(border);

    SwingUtilities.updateComponentTreeUI(myPanel); // TODO: create Swing components in this method (see javadoc)
    myPanel.getTable().setShowGrid(false);

    BorderLayoutPanel myComponent = new BorderLayoutPanel()
      .addToTop(panelWithDescription)
      .addToCenter(myPanel);

    return myComponent;
  }

  @Override
  public void reset() {
    myPanel.getData().clear();
    myPanel.getData().addAll(PythonDocumentationMap.getInstance().getEntries().entrySet());
  }

  @Override
  public boolean isModified() {
    Map<String, String> originalEntries = Map.copyOf(PythonDocumentationMap.getInstance().getEntries());
    Map<String, String> editedEntries = ImmutableMap.copyOf(myPanel.getData());
    return !editedEntries.equals(originalEntries);
  }

  @Override
  public void apply() throws ConfigurationException {
    PythonDocumentationMap.getInstance().setEntries(ImmutableMap.copyOf(myPanel.getData()));
  }

  private static class PythonDocumentationTableModel extends AddEditRemovePanel.TableModel<Map.Entry<String, String>> {
    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
      return columnIndex == 0
             ? PyBundle.message("external.documentation.column.name.module")
             : PyBundle.message("external.documentation.column.name.url.path.pattern");
    }

    @Override
    public Object getField(Map.Entry<String, String> o, int columnIndex) {
      return columnIndex == 0 ? o.getKey() : o.getValue();
    }
  }

  private static final PythonDocumentationTableModel ourModel = new PythonDocumentationTableModel();

  private static class PythonDocumentationPanel extends AddEditRemovePanel<Map.Entry<String, String>> {
    PythonDocumentationPanel() {
      super(ourModel, new ArrayList<>());
      setRenderer(1, new ColoredTableCellRenderer() {
        @Override
        protected void customizeCellRenderer(@NotNull JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
          String text = value == null ? "" : (String) value;
          int pos = 0;
          while(pos < text.length()) {
            int openBrace = text.indexOf('{', pos);
            if (openBrace == -1) openBrace = text.length();
            append(text.substring(pos, openBrace));
            int closeBrace = text.indexOf('}', openBrace);
            if (closeBrace == -1)
              closeBrace = text.length();
            else
              closeBrace++;
            append(text.substring(openBrace, closeBrace), new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, JBColor.BLUE.darker()));
            pos = closeBrace;
          }
        }
      });
    }

    @Override
    protected Map.Entry<String, String> addItem() {
      return showEditor(null);
    }

    private @Nullable Map.Entry<String, String> showEditor(Map.Entry<String, String> entry) {
      PythonDocumentationEntryEditor editor = new PythonDocumentationEntryEditor(this);
      if (entry != null) {
        editor.setEntry(entry);
      }
      editor.show();
      if (editor.getExitCode() != DialogWrapper.OK_EXIT_CODE) {
        return null;
      }
      return editor.getEntry();
    }

    @Override
    protected boolean removeItem(Map.Entry<String, String> o) {
      return true;
    }

    @Override
    protected Map.Entry<String, String> editItem(Map.Entry<String, String> o) {
      return showEditor(o);
    }
  }
}
