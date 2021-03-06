package org.sigmah.shared.command;

import org.sigmah.shared.command.base.AbstractCommand;
import org.sigmah.shared.command.result.CreateResult;
import org.sigmah.shared.dto.PartnerDTO;

/**
 * Adds a {@link org.sigmah.server.domain.OrgUnit} to the the given {@link org.sigmah.server.domain.UserDatabase}.
 *
 * @author Alex Bertram
 * @author Denis Colliot (dcolliot@ideia.fr)
 */
public class AddPartner extends AbstractCommand<CreateResult> {

	private int databaseId;
	private PartnerDTO partner;

	public AddPartner() {
		// Serialization.
	}

	public AddPartner(int databaseId, PartnerDTO partner) {
		this.databaseId = databaseId;
		this.partner = partner;
	}

	public int getDatabaseId() {
		return databaseId;
	}

	public void setDatabaseId(int databaseId) {
		this.databaseId = databaseId;
	}

	public PartnerDTO getPartner() {
		return partner;
	}

	public void setPartner(PartnerDTO partner) {
		this.partner = partner;
	}
}
