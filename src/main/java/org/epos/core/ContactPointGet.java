package org.epos.core;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.epos.eposdatamodel.ContactPoint;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.WebService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.swagger.model.ProviderType;
import metadataapis.ContactPointAPI;
import metadataapis.DataProductAPI;
import metadataapis.DistributionAPI;
import metadataapis.EntityNames;
import metadataapis.WebServiceAPI;
import model.StatusType;
import usermanagementapis.UserGroupManagementAPI;

public class ContactPointGet {

	private static final DistributionAPI distributionAPI = new DistributionAPI(EntityNames.DISTRIBUTION.name(),
			model.Distribution.class);
	private static final DataProductAPI dataProductAPI = new DataProductAPI(EntityNames.DATAPRODUCT.name(),
			DataProduct.class);
	private static final WebServiceAPI webServiceAPI = new WebServiceAPI(EntityNames.WEBSERVICE.name(),
			model.Webservice.class);
	private static final ContactPointAPI contactPointAPI = new ContactPointAPI(EntityNames.CONTACTPOINT.name(),
			model.Contactpoint.class);

	private static final Logger LOGGER = LoggerFactory.getLogger(ContactPointGet.class);

	public static JsonObject generate(JsonObject response, Map<String, Object> requestParams) {

		LOGGER.info("Requests start - JPA method");

		String id = requestParams.get("id").toString();
		ProviderType type = ProviderType.valueOf(requestParams.get("type").toString());

		LOGGER.debug("Id: " + id + "\nType: " + type);

		Distribution distribution = distributionAPI.retrieve(id);

		DataProduct dataProduct = null;
		LOGGER.debug("Distribution: " + distribution);
        if (Objects.nonNull(distribution.getDataProduct())) {
            for (LinkedEntity linkedEntity : distribution.getDataProduct()) {
                DataProduct dp = dataProductAPI.retrieve(linkedEntity.getInstanceId());
                if (dp.getStatus() == StatusType.PUBLISHED) {
                    dataProduct = dp;
                    break;
                }
            }
            LOGGER.debug("DataProduct: " + dataProduct);

            WebService webService = null;
            if (Objects.nonNull(distribution.getAccessService())) {
                for (LinkedEntity linkedEntity : distribution.getAccessService()) {
                    WebService ws = webServiceAPI.retrieve(linkedEntity.getInstanceId());
                    if (ws.getStatus() == StatusType.PUBLISHED) {
                        webService = ws;
                        break;
                    }
                }
                LOGGER.debug("WebService: " + webService);

                JsonArray listEmails = generateEmailList(webService, dataProduct, type);
                if (listEmails == null) {
                    LOGGER.debug("listEmails is null");
                    return response;
                }

                response.add("emails", listEmails);
            }
        }


        LOGGER.info(response.toString());
        return response;
	}

	public static JsonArray generateEmailList(WebService webService, DataProduct dataProduct, ProviderType type) {
		Set<String> emailSet = new HashSet<String>();
		switch (type) {
			case SERVICEPROVIDERS:
				if (webService != null)
					for (LinkedEntity linkedEntity : webService.getContactPoint()) {
						ContactPoint contactPoint = contactPointAPI.retrieve(linkedEntity.getInstanceId());
						for (String email : contactPoint.getEmail()) {
							emailSet.add(email);
						}
					}
				break;
			case DATAPROVIDERS:
				if (dataProduct != null)
					for (LinkedEntity linkedEntity : dataProduct.getContactPoint()) {
						ContactPoint contactPoint = contactPointAPI.retrieve(linkedEntity.getInstanceId());
						for (String email : contactPoint.getEmail()) {
							emailSet.add(email);
						}
					}
				break;
			case ALL:
				if (webService != null)
					for (LinkedEntity linkedEntity : webService.getContactPoint()) {
						ContactPoint contactPoint = contactPointAPI.retrieve(linkedEntity.getInstanceId());
						for (String email : contactPoint.getEmail()) {
							emailSet.add(email);
						}
					}
				if (dataProduct != null)
					for (LinkedEntity linkedEntity : dataProduct.getContactPoint()) {
						ContactPoint contactPoint = contactPointAPI.retrieve(linkedEntity.getInstanceId());
						for (String email : contactPoint.getEmail()) {
							emailSet.add(email);
						}
					}
				break;
			default:
				return null;
		}

		JsonArray listEmails = new JsonArray();
		for (String email : emailSet) {
			listEmails.add(email);
		}
		return listEmails;
	}

	public static JsonArray generateEmailListForRole(String role) {
		Set<String> emailSet = new HashSet<String>();
		List<ContactPoint> allContacts = contactPointAPI.retrieveAll();
		for (ContactPoint contactPoint : allContacts) {
			if (role.equals(contactPoint.getRole())) {
				for (String email : contactPoint.getEmail()) {
					emailSet.add(email);
				}
			}
		}
		JsonArray listEmails = new JsonArray();
		for (String email : emailSet) {
			listEmails.add(email);
		}
		return listEmails;
	}

	public static JsonArray generateEmailListForGroup(String group) {
		Set<String> emailSet = new HashSet<String>();
		LOGGER.info("Resolving recipient emails for group '{}'", group);

		List<Group> groups = UserGroupManagementAPI.retrieveAllGroups();
		Map<String, String> groupMap = groups.stream()
			.collect(Collectors.toMap(Group::getId, Group::getName));
		LOGGER.info("Retrieved {} groups from user management", groupMap.size());

		Optional<String> groupId = groups.stream()
			.filter(g -> g.getName().equals(group))
			.map(Group::getId)
			.findFirst();

		if (groupId.isEmpty()) {
			LOGGER.warn("Group '{}' not found. Available groups are: {}", group, groupMap.values());
			return new JsonArray();
		}

		LOGGER.info("Resolved group '{}' to groupId '{}'. Looking up contact points.", group, groupId.get());

		List<ContactPoint> allContacts = contactPointAPI.retrieveAll();
		LOGGER.info("Retrieved {} contact points from metadata API", allContacts.size());
		int matchingContactPoints = 0;
		int contactsWithoutEmails = 0;
		for (ContactPoint contactPoint : allContacts) {
			if (contactPoint.getGroups() != null && !contactPoint.getGroups().isEmpty() && contactPoint.getGroups().contains(groupId.get())) {
				matchingContactPoints++;
				if (contactPoint.getEmail() == null || contactPoint.getEmail().isEmpty()) {
					contactsWithoutEmails++;
					continue;
				}
				for (String email : contactPoint.getEmail()) {
					emailSet.add(email);
				}
			}
		}
		JsonArray listEmails = new JsonArray();
		for (String email : emailSet) {
			listEmails.add(email);
		}
		LOGGER.info(
				"Group '{}' (id '{}') matched {} contact points ({} without emails) and produced {} unique recipient emails",
				group, groupId.get(), matchingContactPoints, contactsWithoutEmails, listEmails.size());
		return listEmails;
	}
}
