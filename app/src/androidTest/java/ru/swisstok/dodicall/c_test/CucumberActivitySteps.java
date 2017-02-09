package ru.swisstok.dodicall.c_test;

import android.graphics.Bitmap;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;

import java.io.ByteArrayOutputStream;

import cucumber.api.CucumberOptions;
import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.LoginActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

@CucumberOptions(
        format = {
                "pretty",
                "html:/data/data/ru.swisstok.dodicall/html",
                "json:/data/data/ru.swisstok.dodicall/jreport"
        },
        features = "features"
)
public class CucumberActivitySteps extends ActivityInstrumentationTestCase2<LoginActivity> {

    public CucumberActivitySteps() {
        super(LoginActivity.class);
    }

    @Given("^Ошибка не показывается$")
    public void givenErrorTextVisibility() {
        getActivity(); //prevent "Did you forget to launch the activity by calling getActivity()"
        onView(withId(R.id.login_failure)).check(matches(not(isDisplayed())));
    }

    @When("^Пользователь '(.+)' авторизуется в системе с паролем '(.+)'$")
    public void userLogin(String login, String password) {
        onView(withId(R.id.login)).perform(clearText());
        onView(withId(R.id.password)).perform(clearText());
        onView(withId(R.id.login)).perform(typeText(login));
        onView(withId(R.id.password)).perform(typeText(password));
        onView(withId(R.id.login_button)).perform(click());
    }

    @Then("^Появилось сообщение '(.+)'$")
    public void checkErrorText(String msg) {
        onView(withId(R.id.login_failure)).check(matches(isDisplayed()));
        onView(withId(R.id.login_failure)).check(matches(withText(R.string.login_failure)));
    }

    @After
    public void embedScreenshot(Scenario scenario) {
        if (scenario.isFailed()) {
            View view = getActivity().getWindow().getDecorView();
            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            scenario.embed(stream.toByteArray(), "image/png");
        }
    }

}
