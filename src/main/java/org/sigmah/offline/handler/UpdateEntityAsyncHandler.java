package org.sigmah.offline.handler;

import com.extjs.gxt.ui.client.data.RpcMap;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.sigmah.client.dispatch.DispatchListener;
import org.sigmah.offline.dao.PersonalCalendarAsyncDAO;
import org.sigmah.offline.dao.ProjectReportAsyncDAO;
import org.sigmah.offline.dao.UpdateDiaryAsyncDAO;
import org.sigmah.offline.dispatch.AsyncCommandHandler;
import org.sigmah.offline.dispatch.OfflineExecutionContext;
import org.sigmah.shared.command.UpdateEntity;
import org.sigmah.shared.command.result.Authentication;
import org.sigmah.shared.command.result.Calendar;
import org.sigmah.shared.command.result.VoidResult;
import org.sigmah.shared.dto.calendar.CalendarWrapper;
import org.sigmah.shared.dto.calendar.Event;
import org.sigmah.shared.dto.calendar.PersonalCalendarIdentifier;
import org.sigmah.shared.dto.calendar.PersonalEventDTO;
import org.sigmah.shared.dto.report.KeyQuestionDTO;
import org.sigmah.shared.dto.report.ProjectReportContent;
import org.sigmah.shared.dto.report.ProjectReportDTO;
import org.sigmah.shared.dto.report.ProjectReportSectionDTO;
import org.sigmah.shared.dto.report.RichTextElementDTO;

/**
 * JavaScript implementation of {@link org.sigmah.server.handler.UpdateEntityHandler}.
 * Used when the user is offline.
 * 
 * @author Raphaël Calabro (rcalabro@ideia.fr)
 */
public class UpdateEntityAsyncHandler implements AsyncCommandHandler<UpdateEntity, VoidResult>, DispatchListener<UpdateEntity, VoidResult> {

	@Inject
	private UpdateDiaryAsyncDAO updateDiaryAsyncDAO;
	
	@Inject
	private ProjectReportAsyncDAO projectReportAsyncDAO;
	
	@Inject
	private PersonalCalendarAsyncDAO personalCalendarAsyncDAO;
	
	@Override
	public void execute(UpdateEntity command, OfflineExecutionContext executionContext, AsyncCallback<VoidResult> callback) {
		executeCommand(command, executionContext.getAuthentication(), callback);
		updateDiaryAsyncDAO.saveOrUpdate(command);
	}
	
	@Override
	public void onSuccess(UpdateEntity command, VoidResult result, Authentication authentication) {
		executeCommand(command, authentication, null);
	}
	
	private void executeCommand(UpdateEntity command, Authentication authentication, AsyncCallback<VoidResult> callback) {
		if(ProjectReportDTO.ENTITY_NAME.equals(command.getEntityName())) {
			updateProjectReport(command.getId(), command.getChanges(), authentication, callback);
			
		} else if(PersonalEventDTO.ENTITY_NAME.equals(command.getEntityName())) {
			updatePersonalEvent(command.getId(), command.getChanges(), callback);
			
		} else {
			exception(command, callback != null);
		}
	}
	
	private void exception(UpdateEntity command, boolean throwException) throws UnsupportedOperationException {
		if(throwException) {
			throw new UnsupportedOperationException("Creation of type '" + command.getEntityName() + "' is not supported yet.");
		}
	}

	private <T> AsyncCallback<T> wrapVoidResultCallback(final AsyncCallback<VoidResult> callback) {
		if(callback == null) {
			return null;
		}
		return new AsyncCallback<T>() {

			@Override
			public void onFailure(Throwable caught) {
				callback.onFailure(caught);
			}

			@Override
			public void onSuccess(T result) {
				callback.onSuccess(null);
			}
		};
	}
	
	private void updateProjectReport(int entityId, final RpcMap changes, final Authentication authentication, final AsyncCallback<VoidResult> callback) {
		projectReportAsyncDAO.getByVersionId(entityId, new AsyncCallback<ProjectReportDTO>() {

			@Override
			public void onFailure(Throwable caught) {
				callback.onFailure(caught);
			}

			@Override
			public void onSuccess(ProjectReportDTO projectReportDTO) {
				for (final Map.Entry<String, Object> entry : changes.entrySet()) {
					if (ProjectReportDTO.CURRENT_PHASE.equals(entry.getKey())) {
						projectReportDTO.setPhaseName((String) entry.getValue());
						projectReportDTO.setEditorName(authentication.getUserCompleteName());
						projectReportDTO.setLastEditDate(new Date());

					} else {
						final RichTextElementDTO richTextElement = findRichTextElement(Integer.valueOf(entry.getKey()), new ArrayList<ProjectReportContent>(projectReportDTO.getSections()));
						if(richTextElement != null) {
							richTextElement.setText((String) entry.getValue());
						}
					}
				}
				final AsyncCallback<ProjectReportDTO> projectReportCallback = wrapVoidResultCallback(callback);
				projectReportAsyncDAO.saveOrUpdate(projectReportDTO, projectReportCallback);
			}
		});
	}
	
	private RichTextElementDTO findRichTextElement(Integer id, List<ProjectReportContent> contents) {
		for(final ProjectReportContent content : contents) {
			if(content instanceof RichTextElementDTO) {
				final RichTextElementDTO richTextElement = (RichTextElementDTO) content;
				if(id.equals(richTextElement.getId())) {
					return richTextElement;
				}
			} else if(content instanceof ProjectReportSectionDTO) {
				final ProjectReportSectionDTO section = (ProjectReportSectionDTO) content;
				final RichTextElementDTO value = findRichTextElement(id, section.getChildren());
				if(value != null) {
					return value;
				}
			} else if(content instanceof KeyQuestionDTO) {
				final KeyQuestionDTO keyQuestion = (KeyQuestionDTO) content;
				if(id.equals(keyQuestion.getRichTextElementDTO().getId())) {
					return keyQuestion.getRichTextElementDTO();
				}
			}
		}
		return null;
	}
	
	private void updatePersonalEvent(final int entityId, final RpcMap changes, final AsyncCallback<VoidResult> callback) {
		final CalendarWrapper calendarWrapper = (CalendarWrapper) changes.get(Event.CALENDAR_ID);
		final PersonalCalendarIdentifier personalCalendarIdentifier = (PersonalCalendarIdentifier) calendarWrapper.getCalendar().getIdentifier();
		
		personalCalendarAsyncDAO.get(personalCalendarIdentifier.getId(), new AsyncCallback<Calendar>() {

			@Override
			public void onFailure(Throwable caught) {
				if(callback != null) {
					callback.onFailure(caught);
				}
			}

			@Override
			public void onSuccess(Calendar result) {
				boolean done = false;
				final Iterator<Map.Entry<Date, List<Event>>> mapEntryIterator = result.getEvents().entrySet().iterator();
				while(!done && mapEntryIterator.hasNext()) {
					final Map.Entry<Date, List<Event>> entry = mapEntryIterator.next();
					
					final Iterator<Event> eventIterator = entry.getValue().iterator();
					while(!done && eventIterator.hasNext()) {
						final Event event = eventIterator.next();
						if(event.getIdentifier() != null && event.getIdentifier().equals(entityId)) {
							event.fillValues(changes.getTransientMap());
							done = true;
						}
					}
				}
				
				final AsyncCallback<Calendar> calendarCallback = wrapVoidResultCallback(callback);
				personalCalendarAsyncDAO.saveOrUpdate(result, calendarCallback);
			}
		});
	}
}
