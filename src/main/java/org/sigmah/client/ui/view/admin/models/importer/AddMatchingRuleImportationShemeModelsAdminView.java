package org.sigmah.client.ui.view.admin.models.importer;

import org.sigmah.client.i18n.I18N;
import org.sigmah.client.ui.presenter.admin.models.importer.AddMatchingRuleImportationShemeModelsAdminPresenter;
import org.sigmah.client.ui.view.base.AbstractPopupView;
import org.sigmah.client.ui.widget.form.Forms;
import org.sigmah.client.ui.widget.popup.PopupWidget;
import org.sigmah.shared.dto.element.FlexibleElementDTO;
import org.sigmah.shared.dto.importation.VariableDTO;

import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Text;
import com.extjs.gxt.ui.client.widget.form.CheckBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.sigmah.client.ui.widget.button.Button;
import org.sigmah.client.ui.widget.form.FormPanel;

/**
 * @author Mehdi Benabdeslam (mehdi.benabdeslam@netapsys.fr)
 */
@Singleton
public class AddMatchingRuleImportationShemeModelsAdminView extends AbstractPopupView<PopupWidget> implements
																																																	AddMatchingRuleImportationShemeModelsAdminPresenter.View {

	private ComboBox<VariableDTO> variablesCombo;
	private ComboBox<FlexibleElementDTO> flexibleElementsCombo;
	private CheckBox isKeyCheckBox;
	private Text idKeyText;
	private Button submitButton;
	private FlexTable budgetSubFlexTable;

	private FormPanel mainPanel;

	@Inject
	protected AddMatchingRuleImportationShemeModelsAdminView() {
		super(new PopupWidget(true), 500);
	}

	@Override
	public void initialize() {

		mainPanel = Forms.panel();

		// Flexible element combo box.
		flexibleElementsCombo = Forms.combobox(I18N.CONSTANTS.adminFlexible(), true, "id", "label");

		// Variable combo box.
		variablesCombo = Forms.combobox(I18N.CONSTANTS.adminImportVariable(), true, "id", "name");

		// Budget element fields.
		budgetSubFlexTable = new FlexTable();
		budgetSubFlexTable.setCellPadding(2);
		budgetSubFlexTable.setVisible(false);

		isKeyCheckBox = Forms.checkbox(I18N.CONSTANTS.adminImportKeyIdentification());
		isKeyCheckBox.setLabelSeparator("");;
		isKeyCheckBox.disable();
		isKeyCheckBox.hide();

		idKeyText = new Text(I18N.CONSTANTS.adminImportExplicationIdKey());
		idKeyText.hide();

		submitButton = Forms.button(I18N.CONSTANTS.save());

		mainPanel.add(flexibleElementsCombo);
		mainPanel.add(variablesCombo);
		mainPanel.add(budgetSubFlexTable);
		mainPanel.add(isKeyCheckBox);
		mainPanel.add(idKeyText);
		mainPanel.add(submitButton);

		initPopup(mainPanel);

	}

	@Override
	public ComboBox<VariableDTO> getVariablesCombo() {
		return variablesCombo;
	}

	@Override
	public ComboBox<FlexibleElementDTO> getFlexibleElementsCombo() {
		return flexibleElementsCombo;
	}

	@Override
	public CheckBox getIsKeyCheckBox() {
		return isKeyCheckBox;
	}

	@Override
	public Text getIdKeyText() {
		return idKeyText;
	}

	@Override
	public Button getSubmitButton() {
		return submitButton;
	}

	@Override
	public FlexTable getBudgetSubFlexTable() {
		return budgetSubFlexTable;
	}

	@Override
	public void clearForm() {

		budgetSubFlexTable.setVisible(false);
		flexibleElementsCombo.clear();
		variablesCombo.clear();
		getFlexibleElementStore().removeAll();
		getVariableStore().removeAll();

		// TODO ...

	}

	@Override
	public ListStore<FlexibleElementDTO> getFlexibleElementStore() {
		return flexibleElementsCombo.getStore();
	}

	@Override
	public ListStore<VariableDTO> getVariableStore() {
		return variablesCombo.getStore();
	}

	@Override
	public ContentPanel getMainPanel() {
		return mainPanel;
	}

	@Override
	public boolean isValid() {
		return mainPanel.isValid();
	}
	
}
