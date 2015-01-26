package org.sigmah.server.handler;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.sigmah.server.dispatch.impl.UserDispatch.UserExecutionContext;
import org.sigmah.server.domain.OrgUnitModel;
import org.sigmah.server.domain.Phase;
import org.sigmah.server.domain.PhaseModel;
import org.sigmah.server.domain.Project;
import org.sigmah.server.domain.ProjectFunding;
import org.sigmah.server.domain.ProjectModel;
import org.sigmah.server.domain.UserDatabase;
import org.sigmah.server.domain.report.ProjectReport;
import org.sigmah.server.domain.report.ProjectReportVersion;
import org.sigmah.server.domain.report.RichTextElement;
import org.sigmah.server.domain.util.Deleteable;
import org.sigmah.server.domain.value.Value;
import org.sigmah.server.handler.base.AbstractCommandHandler;
import org.sigmah.shared.command.Delete;
import org.sigmah.shared.command.result.VoidResult;
import org.sigmah.shared.dto.OrgUnitModelDTO;
import org.sigmah.shared.dto.PhaseModelDTO;
import org.sigmah.shared.dto.ProjectModelDTO;
import org.sigmah.shared.dto.referential.ProjectModelStatus;

import com.google.gwt.thirdparty.guava.common.base.Objects;
import com.google.inject.persist.Transactional;

/**
 * Handler for {@link Delete} command.
 * 
 * @author Alex Bertram (v1.3)
 * @author Maxime Lombard (mlombard@ideia.fr) (v2.0)
 * @author Raphaël Calabro (rcalabro@ideia.fr)
 * @see org.sigmah.shared.command.Delete
 * @see org.sigmah.server.domain.util.Deleteable
 */
@SuppressWarnings("unchecked")
public class DeleteHandler extends AbstractCommandHandler<Delete, VoidResult> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public VoidResult execute(final Delete cmd, final UserExecutionContext context) {

		// TODO check permissions for delete!
		// These handler should redirect to one of the Entity policy classes.
		@SuppressWarnings("rawtypes")
		final Class entityClass = entityClassForEntityName(cmd.getEntityName());

		performDelete(cmd, entityClass);

		return null;
	}

	/**
	 * Delete the given objets.
	 * 
	 * @param cmd Command defining what to delete.
	 * @param entityClass Type of the entity to delete.
	 */
	@Transactional
	protected void performDelete(final Delete cmd, final Class entityClass) {
		if (ProjectModelStatus.DRAFT.equals(cmd.getProjectModelStatus()) && ProjectModelDTO.ENTITY_NAME.equals(cmd.getEntityName())) {
			// Delete draft project model
			final ProjectModel projectModel = (ProjectModel) em().find(entityClass, cmd.getId());
			deleteProjectModelWithDate(projectModel);
			deleteDraftProjectModel(projectModel);

		} else if (PhaseModelDTO.ENTITY_NAME.equals(cmd.getEntityName())) {
			final PhaseModel phaseModel = em().find(PhaseModel.class, cmd.getId());
			deletePhaseModel(phaseModel);

		} else if (cmd.getProjectModelStatus() == ProjectModelStatus.DRAFT && OrgUnitModelDTO.ENTITY_NAME.equals(cmd.getEntityName())) {
			// Delete draft OrgUnit model
			OrgUnitModel orgUnitModel = em().find(OrgUnitModel.class, cmd.getId());
			deleteOrgUnitModelWithDate(orgUnitModel);
		}

		else {
			final Deleteable entity = (Deleteable) em().find(entityClass, cmd.getId());
			entity.delete();
			em().persist(entity);
		}
	}

	private void deletePhaseModel(PhaseModel phaseModel) {

		// ----STEP1: delete the successor relation---------------------------

		// If this model is the successor of other phase model,
		// this relation should be removed first

		Query query = em().createQuery("FROM PhaseModel ");
		List<PhaseModel> models = query.getResultList();
		for (PhaseModel p : models) {
			if (p.getSuccessors() != null) {
				if (p.getSuccessors().contains(phaseModel)) {
					p.getSuccessors().remove(phaseModel);
					em().merge(p);
				}
			}

		}

		// ----STEP2: delete all child phases using this phase model-----------
		final TypedQuery<Phase> queryPhases = em().createQuery("SELECT p FROM Phase p WHERE p.phaseModel.id = :phaseModelId", Phase.class);
		queryPhases.setParameter("phaseModelId", phaseModel.getId());

		for (final Phase phase : queryPhases.getResultList()) {
			if (phase.getParentProject() != null && Objects.equal(phase.getParentProject().getCurrentPhase(), phase)) {
				final Project parentProject = em().find(Project.class, phase.getParentProject().getId());
				parentProject.setCurrentPhase(null);
				em().merge(parentProject);
			}
			em().remove(phase);
		}

		// -----STEP3: delete the phase
		// model-------------------------------------------

		em().remove(phaseModel);
		em().flush();
		em().clear();

	}

	private Class<Deleteable> entityClassForEntityName(String entityName) {
		try {
			return (Class<Deleteable>) Class.forName(UserDatabase.class.getPackage().getName() + "." + entityName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Invalid entity name '" + entityName + "'");
		} catch (ClassCastException e) {
			throw new RuntimeException("Entity type '" + entityName + "' not Deletable");
		}
	}

	/**
	 * Delete the test project object.
	 * 
	 * @param project
	 *          the object to delete.
	 * @deprecated Use {@link #deleteProjectWithDate(Project)}
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private void deleteTestProject(Project project) {
		// delete the project flexible elem()ents
		deleteProjectFlexibleElement(project);

		// delete the test project
		em().remove(project);
	}

	/**
	 * Delete the project object.
	 * 
	 * @param project
	 *          the object to delete.
	 * @deprecated Use {@link #deleteProjectWithDate(Project)}
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private void deleteProject(Project project) {
		// delete the project flexible elem()ents
		deleteProjectFlexibleElement(project);

		// delete the test project
		em().remove(project);
	}

	/**
	 * Sets the deleted date of the given project.
	 * 
	 * @param project
	 *          the project to delete.
	 */
	private void deleteProjectWithDate(Project project) {
		project.delete();

		// Deletes all the links of the project (avoids orphan links)
		final List<ProjectFunding> listfundingsToDelete = new ArrayList<ProjectFunding>();

		listfundingsToDelete.addAll(project.getFunded());
		listfundingsToDelete.addAll(project.getFunding());

		project.getFunded().clear();
		project.getFunding().clear();

		for (ProjectFunding pf : listfundingsToDelete) {
			em().remove(pf);
		}
	}

	/**
	 * Sets the deleted date of the given project model.
	 * 
	 * @param projectModel
	 *          the project model to delete.
	 */
	private void deleteProjectModelWithDate(ProjectModel projectModel) {
		projectModel.delete();
	}

	/**
	 * Sets the deleted date of the given org unit model.
	 * 
	 * @param orgUnitModel
	 *          the org unit model to delete.
	 */
	private void deleteOrgUnitModelWithDate(OrgUnitModel orgUnitModel) {
		orgUnitModel.delete();
	}

	/**
	 * Delete the values of the test project.
	 * 
	 * @param project
	 * @deprecated Use {@link #deleteProjectWithDate(Project)}
	 */
	@Deprecated
	private void deleteProjectFlexibleElement(Project project) {
		// delete values
		Query query = em().createQuery("Select v FROM Value v WHERE v.containerId =:containerId");
		query.setParameter("containerId", project.getId());
		final List<Value> listResultsValues = query.getResultList();
		if (listResultsValues != null) {
			for (Value value : listResultsValues) {
				em().remove(value);
			}
		}

		// delete project reports
		query = em().createQuery("Select pr FROM ProjectReport pr WHERE pr.project.id =:databaseid");
		query.setParameter("databaseid", project.getId());
		final List<ProjectReport> listResultReports = query.getResultList();
		if (listResultReports != null) {
			for (ProjectReport report : listResultReports) {
				// Delete the project report's version
				ProjectReportVersion version = report.getCurrentVersion();
				if (version != null) {
					// delete vercion's richText elem()ents
					List<RichTextElement> richTextElements = version.getTexts();
					if (richTextElements != null) {
						for (RichTextElement richTextElement : richTextElements) {
							em().remove(richTextElement);
						}
					}

					em().remove(version);
				}
				em().remove(report);
			}
		}
	}

	/**
	 * Method to delete a project model. Only draft project model is allowed to delete.
	 * 
	 * @param projectModel
	 * @author HUZHE(zhe.hu32@gmail.com)
	 */
	private void deleteDraftProjectModel(ProjectModel projectModel) {

		// ------STEP 1: Get all projects using this project model and delete
		// them()------------
		final Query query = em().createQuery("SELECT p FROM Project p WHERE p.projectModel=:model");
		query.setParameter("model", projectModel);
		List<Project> projects = query.getResultList();

		for (Project p : projects) {
			deleteProjectWithDate(p);
		}

		em().flush();
	}

}