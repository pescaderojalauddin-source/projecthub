package com.example.projecthub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MentionsServiceTest {

    @Mock
    UserRepository userRepository;

    MentionsService svc;

    @BeforeEach
    void setUp() {
    // в репозитории два юзера: ivan и maria
        when(userRepository.findAll()).thenReturn(List.of(
                new User("ivan", "h", Role.USER),
                new User("maria", "h", Role.USER)
        ));
        svc = new MentionsService(userRepository);
    }

    @Test
    void nullAndBlankReturnEmpty() {
        assertThat(svc.render(null)).isEmpty();
        assertThat(svc.render("")).isEmpty();
        assertThat(svc.render("   ")).isEmpty();
    }

    @Test
    void existingLoginIsWrappedInMentionLink() {
        String html = svc.render("Привет @ivan!");
        assertThat(html).contains("class=\"mention\"");
        assertThat(html).contains("href=\"/profile/ivan\"");
        assertThat(html).contains("@ivan");
    }

    @Test
    void unknownLoginStaysAsPlainText() {
        String html = svc.render("Привет @nobody42!");
        assertThat(html).contains("@nobody42");
        assertThat(html).doesNotContain("class=\"mention\">@nobody42");
    }

    @Test
    void caseInsensitiveMatch() {
    // юзер ivan в нижнем регистре — @IVAN всё равно матчится
        String html = svc.render("@IVAN");
        assertThat(html).contains("class=\"mention\"");
    }

    @Test
    void dangerousHtmlInInputIsEscaped() {
        String html = svc.render("<script>alert('x')</script>");
        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;");
    }

    @Test
    void atInsideEmailIsNotTreatedAsMention() {
    // нет @ после слова — это уже email-подобный кейс. regex смотрит lookbehind
    // (?<![\\p{L}\\p{N}_])@ — поэтому foo@bar НЕ матчит
        String html = svc.render("foo@bar");
        assertThat(html).doesNotContain("class=\"mention\"");
    }

    @Test
    void newlinesBecomeBrTags() {
        String html = svc.render("line1\nline2");
        assertThat(html).contains("<br/>");
    }

    @Test
    void multipleMentionsInOneText() {
        String html = svc.render("@ivan и @maria — рулят");
        assertThat(html).contains("href=\"/profile/ivan\"");
        assertThat(html).contains("href=\"/profile/maria\"");
    }

    @Test
    void invalidateClearsCache() {
    // первый вызов прогревает кэш
        svc.render("@ivan");
    // инвалидируем — следующий вызов снова сходит в репо
        svc.invalidate();
        String html = svc.render("@ivan");
        assertThat(html).contains("class=\"mention\"");
    }
}
