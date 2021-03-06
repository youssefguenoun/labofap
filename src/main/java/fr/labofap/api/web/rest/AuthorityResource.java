package fr.labofap.api.web.rest;

import com.codahale.metrics.annotation.Timed;
import fr.labofap.api.domain.Authority;
import fr.labofap.api.security.AuthoritiesConstants;
import fr.labofap.api.service.UserService;
import fr.labofap.api.web.rest.errors.BadRequestAlertException;
import fr.labofap.api.web.rest.util.HeaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * REST controller for managing users.
 * <p>
 * This class accesses the User entity, and needs to fetch its collection of authorities.
 * <p>
 * For a normal use-case, it would be better to have an eager relationship between User and Authority,
 * and send everything to the client side: there would be no View Model and DTO, a lot less code, and an outer-join
 * which would be good for performance.
 * <p>
 * We use a View Model and a DTO for 3 reasons:
 * <ul>
 * <li>We want to keep a lazy association between the user and the authorities, because people will
 * quite often do relationships with the user, and we don't want them to get the authorities all
 * the time for nothing (for performance reasons). This is the #1 goal: we should not impact our users'
 * application because of this use-case.</li>
 * <li> Not having an outer join causes n+1 requests to the database. This is not a real issue as
 * we have by default a second-level cache. This means on the first HTTP call we do the n+1 requests,
 * but then all authorities come from the cache, so in fact it's much better than doing an outer join
 * (which will get lots of data from the database, for each HTTP call).</li>
 * <li> As this manages users, for security reasons, we'd rather have a DTO layer.</li>
 * </ul>
 * <p>
 * Another option would be to have a specific JPA entity graph to handle this case.
 */
@RestController
@RequestMapping("/api")
public class AuthorityResource {

    private final Logger log = LoggerFactory.getLogger(AuthorityResource.class);

    private final UserService userService;


    public AuthorityResource(UserService userService) {

        this.userService = userService;
    }

    @PostMapping("/authorities")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Authority> createAuthority(@Valid @RequestBody String authorityName) throws URISyntaxException {
        log.debug("REST request to save Authority : {}", authorityName);

        if (StringUtils.isEmpty(authorityName)) {
            throw new BadRequestAlertException("A new Authority cannot be null or empty", "Authority", "idexists");
            // Lowercase the user login before comparing with database
        }

        Authority authority = userService.registerAuthority(authorityName);

        return ResponseEntity.created(new URI("/api/authorities/" + authorityName))
            .headers(HeaderUtil.createAlert( "authorities.created", authorityName))
            .body(authority);
    }
}
