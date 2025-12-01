import Keycloak from "keycloak-js";

const keycloak = new Keycloak({
    url: "https://ron-site.iptime.org/",
    realm: "test",
    clientId: "account"
});

export default keycloak;
