package org.sigmah.server.handler;

import java.util.List;

import javax.persistence.Query;

import org.sigmah.server.dispatch.impl.UserDispatch.UserExecutionContext;
import org.sigmah.server.domain.element.FlexibleElement;
import org.sigmah.server.domain.layout.LayoutConstraint;
import org.sigmah.server.handler.base.AbstractCommandHandler;
import org.sigmah.shared.command.DeleteFlexibleElements;
import org.sigmah.shared.command.result.VoidResult;
import org.sigmah.shared.dispatch.CommandException;
import org.sigmah.shared.dto.element.FlexibleElementDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Handler for {@link DeleteFlexibleElements} command
 * 
 * @author Maxime Lombard (mlombard@ideia.fr)
 */
public class DeleteFlexibleElementsHandler extends AbstractCommandHandler<DeleteFlexibleElements, VoidResult> {

	/**
	 * Logger.
	 */
	private final static Logger LOG = LoggerFactory.getLogger(DeleteFlexibleElementsHandler.class);

	@Inject
	public DeleteFlexibleElementsHandler() {

	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public VoidResult execute(final DeleteFlexibleElements cmd, final UserExecutionContext context) throws CommandException {

		if (cmd.getFlexibleElements() != null) {
			for (FlexibleElementDTO flexEltDTO : cmd.getFlexibleElements()) {

				FlexibleElement flexElt = em().find(FlexibleElement.class, flexEltDTO.getId());

				Query query = em().createQuery("Select l from LayoutConstraint l Where l.element = :flexibleElement");
				query.setParameter("flexibleElement", flexElt);
				for (LayoutConstraint layout : (List<LayoutConstraint>) query.getResultList()) {
					em().remove(layout);
				}
				LOG.debug("DeactivateUsersHandler flexElt " + flexEltDTO.getId() + " name" + flexEltDTO.getLabel());

				em().remove(flexElt);
			}
		}

		return null;
	}

}
