/*******************************************************************************
 * Copyright (c) 2012 - 2015 hangum.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     hangum - initial API and implementation
 ******************************************************************************/
package com.hangum.tadpole.manager.core.dialogs.api;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.gson.JsonArray;
import com.hangum.tadpole.commons.google.analytics.AnalyticCaller;
import com.hangum.tadpole.commons.libs.core.define.PublicTadpoleDefine;
import com.hangum.tadpole.commons.util.JSONUtil;
import com.hangum.tadpole.commons.util.download.DownloadServiceHandler;
import com.hangum.tadpole.commons.util.download.DownloadUtils;
import com.hangum.tadpole.engine.query.dao.ResourceManagerDAO;
import com.hangum.tadpole.engine.query.dao.system.UserDBDAO;
import com.hangum.tadpole.engine.restful.RESTfulAPIUtils;
import com.hangum.tadpole.engine.sql.paremeter.NamedParameterDAO;
import com.hangum.tadpole.engine.sql.paremeter.NamedParameterUtil;
import com.hangum.tadpole.engine.sql.util.QueryUtils;
import com.hangum.tadpole.engine.sql.util.SQLUtil;
import com.hangum.tadpole.manager.core.Messages;

/**
 * Test API service dialog
 *
 * @author hangum
 * @version 1.6.1
 * @since 2015. 5. 19.
 *
 */
public class APIServiceDialog extends Dialog {
	private static final Logger logger = Logger.getLogger(APIServiceDialog.class);
	
	/** DOWNLOAD BUTTON ID */
	private int DOWNLOAD_BTN_ID = IDialogConstants.CLIENT_ID + 1;
	
	private UserDBDAO userDB;
	private String strSQL = ""; //$NON-NLS-1$
	private ResourceManagerDAO resourceManagerDao;
	
	private Text textAPIName;
	private Text textArgument;
	private Text textResult;
	
	private Combo comboResultType;
	
	private Button btnAddHeader;
	private Text textDelimiter;
	
	/** download servcie handler. */
	private DownloadServiceHandler downloadServiceHandler;
	private Text textApiURL;

	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public APIServiceDialog(Shell parentShell, UserDBDAO userDB, String strSQL, ResourceManagerDAO resourceManagerDao) {
		super(parentShell);
		setShellStyle(SWT.MAX | SWT.RESIZE | SWT.TITLE);
		
		this.userDB = userDB;
		this.strSQL = strSQL;
		this.resourceManagerDao = resourceManagerDao;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("RESTful API test Dialog"); //$NON-NLS-1$
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		
		Composite compositeTitle = new Composite(container, SWT.NONE);
		compositeTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		compositeTitle.setLayout(new GridLayout(2, false));
		
		Label lblApiName = new Label(compositeTitle, SWT.NONE);
		lblApiName.setText("API NAME"); //$NON-NLS-1$
		
		textAPIName = new Text(compositeTitle, SWT.BORDER);
		textAPIName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		textAPIName.setEnabled(false);
		
		Label lblApiUrl = new Label(compositeTitle, SWT.NONE);
		lblApiUrl.setText("API URL"); //$NON-NLS-1$
		
		textApiURL = new Text(compositeTitle, SWT.BORDER);
		textApiURL.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		textApiURL.setEnabled(false);
		
		Label lblArgument = new Label(compositeTitle, SWT.NONE);
		lblArgument.setText("Argument"); //$NON-NLS-1$
		
		textArgument = new Text(compositeTitle, SWT.BORDER);
		textArgument.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label lblType = new Label(compositeTitle, SWT.NONE);
		lblType.setText("Result Type"); //$NON-NLS-1$
		
		comboResultType = new Combo(compositeTitle, SWT.READ_ONLY);
		comboResultType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean isEnable = false;
				if(QueryUtils.RESULT_TYPE.CSV.name().equals(comboResultType.getText())) {
					isEnable = true;
				}
				
				btnAddHeader.setEnabled(isEnable);
				textDelimiter.setEnabled(isEnable);
			}
		});
		comboResultType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		for (QueryUtils.RESULT_TYPE resultType : QueryUtils.RESULT_TYPE.values()) {
			comboResultType.add(resultType.name());
		}
		comboResultType.select(1);
		new Label(compositeTitle, SWT.NONE);
		
		Composite compositeDetailCSV = new Composite(compositeTitle, SWT.NONE);
		compositeDetailCSV.setLayout(new GridLayout(3, false));
		compositeDetailCSV.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		btnAddHeader = new Button(compositeDetailCSV, SWT.CHECK);
		btnAddHeader.setText("Add Header"); //$NON-NLS-1$
		
		Label lblDelimiter = new Label(compositeDetailCSV, SWT.NONE);
		lblDelimiter.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblDelimiter.setText("Delimiter"); //$NON-NLS-1$
		
		textDelimiter = new Text(compositeDetailCSV, SWT.BORDER);
		textDelimiter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Group grpResultSet = new Group(container, SWT.NONE);
		grpResultSet.setLayout(new GridLayout(1, false));
		grpResultSet.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		grpResultSet.setText("Result Set"); //$NON-NLS-1$
		
		textResult = new Text(grpResultSet, SWT.BORDER | SWT.WRAP | SWT.H_SCROLL | SWT.CANCEL | SWT.MULTI);
		textResult.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		initUI();

		registerServiceHandler();
		
		AnalyticCaller.track("APIServiceDialog"); //$NON-NLS-1$
		
		return container;
	}
	
	/**
	 * initialize UI
	 */
	private void initUI() {
		textAPIName.setText(resourceManagerDao.getName());
		textApiURL.setText(resourceManagerDao.getRestapi_uri());
		
		textArgument.setText(RESTfulAPIUtils.getParameter(strSQL));
		btnAddHeader.setSelection(true);
		textDelimiter.setText(","); //$NON-NLS-1$
	}
	
	/**
	 * initialize ui
	 * 
	 * @param strArgument
	 */
	private void initData(String strArgument) {
		
		try {
			String strReturnResult = "";
			
			// velocity 로 if else 가 있는지 검사합니다. 
			String strSQLs = RESTfulAPIUtils.makeTemplateTOSQL("APIServiceDialog", strSQL, strArgument);
			// 분리자 만큼 실행한다.
			for (String strTmpSQL : strSQLs.split(PublicTadpoleDefine.SQL_DELIMITER)) {

				NamedParameterDAO dao = NamedParameterUtil.parseParameterUtils(strTmpSQL, strArgument);
				if(QueryUtils.RESULT_TYPE.JSON.name().equalsIgnoreCase(comboResultType.getText())) {
					strReturnResult += getSelect(userDB, dao.getStrSQL(), dao.getListParam()) + ",";
				} else {
					strReturnResult += getSelect(userDB, dao.getStrSQL(), dao.getListParam());
				}
			}
			
			if(QueryUtils.RESULT_TYPE.JSON.name().equalsIgnoreCase(comboResultType.getText())) {
				strReturnResult = "[" + StringUtils.removeEnd(strReturnResult, ",") + "]"; 
			}
			
			textResult.setText(strReturnResult);
			
		} catch (Exception e) {
			logger.error("api exception", e); //$NON-NLS-1$
			
			MessageDialog.openError(getShell(), "Error", Messages.APIServiceDialog_11 + "\n" + e.getMessage()); //$NON-NLS-1$
		}
	}
	
	/**
	 * called sql
	 * 
	 * @param userDB
	 * @param strSQL
	 * @param listParam
	 * @return
	 * @throws Exception
	 */
	private String getSelect(final UserDBDAO userDB, String strSQL, List<Object> listParam) throws Exception {
		String strResult = ""; //$NON-NLS-1$
		
		if(SQLUtil.isStatement(strSQL)) {
			
			if(QueryUtils.RESULT_TYPE.JSON.name().equals(comboResultType.getText())) {
				JsonArray jsonArry = QueryUtils.selectToJson(userDB, strSQL, listParam);
				strResult = JSONUtil.getPretty(jsonArry.toString());
			} else if(QueryUtils.RESULT_TYPE.CSV.name().equals(comboResultType.getText())) {
				strResult = QueryUtils.selectToCSV(userDB, strSQL, listParam, btnAddHeader.getSelection(), textDelimiter.getText());
			} else if(QueryUtils.RESULT_TYPE.XML.name().equals(comboResultType.getText())) {
				strResult = QueryUtils.selectToXML(userDB, strSQL, listParam);
			} else {
				strResult = QueryUtils.selectToHTML_TABLE(userDB, strSQL, listParam);
			}
		} else {
			strResult = QueryUtils.executeDML(userDB, strSQL, listParam, comboResultType.getText());
		}
		
		return strResult;
	}

	/** download service handler call */
	private void unregisterServiceHandler() {
		RWT.getServiceManager().unregisterServiceHandler(downloadServiceHandler.getId());
		downloadServiceHandler = null;
	}
	
	@Override
	public boolean close() {
		try {
			unregisterServiceHandler();
		} catch(Exception e) {
			logger.error("unregisterServiceHandler", e); //$NON-NLS-1$
		}
		return super.close();
	}

	/**
	 * download external file
	 * 
	 * @param fileName
	 * @param newContents
	 */
	public void downloadExtFile(String fileName, String newContents) {
		downloadServiceHandler.setName(fileName);
		downloadServiceHandler.setByteContent(newContents.getBytes());
		
		DownloadUtils.provideDownload(textDelimiter.getParent(), downloadServiceHandler.getId());
	}
	
	/** registery service handler */
	private void registerServiceHandler() {
		downloadServiceHandler = new DownloadServiceHandler();
		RWT.getServiceManager().registerServiceHandler(downloadServiceHandler.getId(), downloadServiceHandler);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		// download 
		if(buttonId == DOWNLOAD_BTN_ID) {
			String strResult = textResult.getText();
			downloadExtFile("TadpoleAPIServer.txt", strResult); //$NON-NLS-1$
		} else {
			super.buttonPressed(buttonId);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		initData(textArgument.getText());
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, "Close", false); //$NON-NLS-1$
		createButton(parent, DOWNLOAD_BTN_ID, "Download", false); //$NON-NLS-1$
		createButton(parent, IDialogConstants.OK_ID, "RUN", true); //$NON-NLS-1$
	}
	
	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(650, 500);
	}
}
