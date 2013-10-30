package fi.iki.dezgeg.matkakorttiwidget.matkakortti;

import com.gistlabs.mechanize.MechanizeAgent;
import com.gistlabs.mechanize.document.Document;
import com.gistlabs.mechanize.document.html.HtmlDocument;
import com.gistlabs.mechanize.document.html.HtmlElement;
import com.gistlabs.mechanize.document.html.HtmlNode;
import com.gistlabs.mechanize.document.html.form.Form;
import com.gistlabs.mechanize.document.html.form.FormElement;
import com.gistlabs.mechanize.document.html.form.SubmitButton;

import org.apache.http.impl.client.AbstractHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gistlabs.mechanize.document.html.query.HtmlQueryBuilder.byId;
import static com.gistlabs.mechanize.document.html.query.HtmlQueryBuilder.byTag;

public class MatkakorttiApi {
    private String username;
    private String password;

    public static final String OMAMATKAKORTTI_URL_BASE = "https://omamatkakortti.hsl.fi";

    public static final Pattern CARDS_JSON_PATTERN = Pattern.compile(".*?parseJSON\\('(.*)'\\).*", Pattern.DOTALL);
    public static final Pattern DATE_PATTERN = Pattern.compile("^/Date\\(([0-9]+)\\)/$");
    public static final Pattern SESSION_EXISTS_PATTERN = Pattern.compile(".*on toinen avoin istunto.*"); // TODO english

    public MatkakorttiApi(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public List<Card> getCards() throws Exception {
        MechanizeAgent agent;
        try {
            agent = login();
        } catch (MatkakorttiException me) {
            if (!me.isInternalError() &&
                    SESSION_EXISTS_PATTERN.matcher(me.getMessage()).matches()) // Try again once
                agent = login();
            else
                throw me;
        }

        // TODO: Follow redirects
        HtmlDocument response = safeGet(agent, "/Basic/Cards.aspx");
        List<HtmlElement> scripts = response.htmlElements().getAll(byTag("script"));

        List<Card> cards = new ArrayList<Card>();
        for (HtmlElement script : scripts) {
            Matcher matcher = CARDS_JSON_PATTERN.matcher(script.getInnerHtml());
            if (matcher.matches()) {
                JSONArray cardsJson;
                try {
                    cardsJson = new JSONArray(matcher.group(1));
                } catch (JSONException jsone) {
                    throw new MatkakorttiException("JSON in page is not valid", jsone, true);
                }

                for (int i = 0; i < cardsJson.length(); i++) {
                    try {
                        cards.add(createCardFromJSON(cardsJson.getJSONObject(i)));
                    } catch (JSONException jsone) {
                        throw new MatkakorttiException("Error when parsing card JSON", jsone, true);
                    }
                }
                return cards;
            }
        }
        throw new MatkakorttiException("Couldn't locate JSON data in OmaMatkakortti response.", true);
    }

    private HtmlDocument safeGet(MechanizeAgent agent, String url) {
        try {
            return agent.get(OMAMATKAKORTTI_URL_BASE + url);
        } catch (ClassCastException cce) {
            // MechanizeAgent throws this if not text/html
            throw new MatkakorttiException("Page " + url + " not HTML", cce, true);
        }
    }

    private MechanizeAgent login() throws Exception {
        AbstractHttpClient httpClient = NonverifyingSSLSocketFactory.createNonverifyingHttpClient();

        MechanizeAgent agent = new MechanizeAgent(httpClient);
        Document page = safeGet(agent, "/Login.aspx");

        Form loginForm = page.forms().get(byId("aspnetForm"));
        if (loginForm == null)
            throw new MatkakorttiException("Couldn't find aspnetForm", true);


        findField(loginForm, "UserName").set(username);
        findField(loginForm, "Password").set(password);
        FormElement loginButton = findField(loginForm, "LoginButton");
        if (!(loginButton instanceof SubmitButton))
            throw new MatkakorttiException("LoginButton is not a SubmitButton", true);

        HtmlDocument loginResponse = loginForm.submit((SubmitButton) loginButton);
        HtmlElement validationSummary = loginResponse.htmlElements().get(byId("Etuile_mainValidationSummary"));

        if (validationSummary != null) {
            HtmlElement errorList = validationSummary.get(byTag("ul"));
            if (errorList == null)
                throw new MatkakorttiException("Login response has validation summary but no error list", true);
            String errors = "";
            // - 1 since the service will always complain about our browser.
            for (int i = 0; i < errorList.getChildren().size() - 1; i++) {
                HtmlNode elem = errorList.getChildren().get(i);
                if (elem instanceof HtmlElement)
                    errors += ((HtmlElement) elem).getText() + "\n";
            }
            throw new MatkakorttiException(errors, false);
        }
        return agent;
    }

    private FormElement findField(Form loginForm, String name) {
        final String PREFIX = "Etuile$MainContent$LoginControl$LoginForm$";
        FormElement formElement = loginForm.get(PREFIX + name);

        if (formElement == null)
            throw new MatkakorttiException("Couldn't find field " + name + " in login form", true);
        return formElement;
    }

    private Card createCardFromJSON(JSONObject card) throws JSONException {
        double moneyAsDouble = card.getDouble("RemainingMoney");
        // Round to BigDecimal cents safely
        BigDecimal money = new BigDecimal((int) Math.round(100 * moneyAsDouble)).divide(new BigDecimal(100));

        String expiryDateStr = card.getJSONObject("PeriodProductState").getString("ExpiringDate");
        Date expiryDate = null;
        if (!expiryDateStr.equals("null")) {
            Matcher expiryDateMatch = DATE_PATTERN.matcher(expiryDateStr);
            if (!expiryDateMatch.matches()) {
                throw new MatkakorttiException("Date pattern did not match regex: " + expiryDateStr, true);
            }
            expiryDate = new Date(Long.parseLong(expiryDateMatch.group(1)));
        }

        return new Card(card.getString("name"), card.getString("id"), money, expiryDate);
    }
}
