package com.securepwgen.service;

import com.securepwgen.exception.PasswordGenerationException;
import com.securepwgen.model.PasswordRequest;
import com.securepwgen.model.PasswordResponse;
import com.securepwgen.model.PasswordResponse.PasswordEntry;
import com.securepwgen.model.PasswordResponse.StrengthLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Cryptographically secure password generation using Java SecureRandom.
 *
 * Key security decisions:
 * 1. SecureRandom seeds from OS entropy pool — never java.util.Random (48-bit LCG, predictable).
 * 2. nextInt(bound) uses rejection sampling — eliminates modulo bias.
 * 3. Fisher-Yates shuffle — prevents positional prediction of required characters.
 * 4. At least one character from each enabled set is guaranteed before shuffle.
 */
@Service
public class PasswordGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(PasswordGeneratorService.class);

    static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    static final String NUMBERS   = "0123456789";
    static final String SPECIAL   = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    static final String AMBIGUOUS = "0O1lI";

    // Subset of EFF Large Wordlist (production: load full 7776-word list from file)
    private static final String[] WORDLIST = {
        "abacus","abbey","absorb","accent","access","accord","acorn","action",
        "active","actor","adapt","admit","adult","advice","afford","afraid",
        "agency","agree","alarm","alert","allow","almond","alone","alter",
        "amber","amble","angel","angle","animal","apple","april","apron",
        "ardent","argue","armor","aroma","arrow","atlas","audio","avoid",
        "award","azure","badge","baker","basic","batch","beard","begin",
        "bench","berry","blaze","blend","block","bloom","blues","board",
        "boost","brain","brave","bread","break","breed","brick","brief",
        "bring","broad","brook","brown","build","bunch","burst","camel",
        "candy","cedar","chain","chair","charm","chart","chase","cheap",
        "chess","chest","chief","child","claim","clean","clear","cliff",
        "climb","clock","cloud","coast","color","coral","count","court",
        "craft","crane","cross","crowd","crown","cubic","cycle","daisy",
        "dance","delta","depth","digit","dodge","donor","doubt","draft",
        "drain","drama","drink","drive","drums","eagle","earth","elite",
        "enter","equal","error","essay","event","exact","extra","fancy",
        "fault","feast","fence","fever","field","final","flame","flash",
        "fleet","flood","floor","fluid","flute","focus","force","forge",
        "forum","found","frame","fresh","front","fruit","given","glass",
        "globe","gloom","glory","glove","grace","grade","grain","grand",
        "grant","grasp","grass","great","green","grief","grill","grove",
        "guard","guide","guild","happy","harsh","haven","hedge","herbs",
        "holly","honor","horse","house","human","humor","hurry","image",
        "index","inner","input","irony","jazzy","jewel","judge","juicy",
        "knife","known","label","large","laser","laugh","layer","learn",
        "leave","legal","lemon","level","light","limit","local","lodge",
        "logic","loyal","lucky","magic","major","maple","march","match",
        "media","merge","merit","metal","might","minor","model","money",
        "mount","mouth","movie","music","nerve","night","noble","noise",
        "north","novel","nurse","ocean","offer","olive","orbit","order",
        "paint","panel","paper","party","peace","pearl","phase","photo",
        "piano","pilot","pixel","pizza","place","plant","plaza","point",
        "poker","power","press","price","pride","prime","prize","probe",
        "prose","proud","pulse","punch","queen","quest","quick","quiet",
        "quote","radar","radio","raise","range","rapid","reach","react",
        "realm","rebel","reign","relax","renew","reset","rider","ridge",
        "right","river","robot","rocky","rough","round","royal","ruler",
        "saint","salad","sauce","scale","scene","scope","score","scout",
        "sense","seven","shame","shape","share","shark","sharp","shell",
        "shift","shirt","shock","shore","short","shout","sight","since",
        "skill","skull","sleep","slide","smart","smile","smoke","solar",
        "solid","solve","sound","south","space","spark","speak","spend",
        "sport","squad","staff","stage","stand","start","state","steam",
        "steep","stick","still","stone","storm","story","study","style",
        "sugar","sunny","super","surge","swift","sword","table","taste",
        "tempo","theme","thick","thing","think","tiger","title","today",
        "token","touch","tough","tower","track","trade","train","trial",
        "tribe","trick","truck","trust","truth","tulip","twist","ultra",
        "under","union","until","urban","usual","vault","vapor","video",
        "vigor","viral","vista","vital","vivid","voter","water","weird",
        "wheat","wheel","white","whole","width","witch","woman","woods",
        "world","worry","worth","wrote","young","zebra","zesty","zones"
    };

    private final SecureRandom           secureRandom = new SecureRandom();
    private final EntropyCalculatorService entropyService;
    private final BCryptPasswordEncoder  bcrypt;

    public PasswordGeneratorService(EntropyCalculatorService entropyService,
                                    BCryptPasswordEncoder bcrypt) {
        this.entropyService = entropyService;
        this.bcrypt         = bcrypt;
    }

    // ── Public API ────────────────────────────────────────────────────

    public PasswordResponse generate(PasswordRequest req) {
        if (!req.hasAtLeastOneCharSet()) {
            throw new PasswordGenerationException(
                "At least one character set must be enabled.");
        }

        List<PasswordEntry> entries = new ArrayList<>();
        for (int i = 0; i < req.getCount(); i++) {
            entries.add(req.isPassphrase()
                ? buildPassphraseEntry(req)
                : buildPasswordEntry(req));
        }

        log.info("Generated {} {}(s) [length={}, passphrase={}]",
            req.getCount(),
            req.isPassphrase() ? "passphrase" : "password",
            req.getLength(),
            req.isPassphrase());

        return new PasswordResponse(entries);
    }

    // ── Password ──────────────────────────────────────────────────────

    private PasswordEntry buildPasswordEntry(PasswordRequest req) {
        String charset  = buildCharset(req);
        String password = generateFromCharset(charset, req.getLength(), req);

        double       entropy  = entropyService.calculateEntropy(charset.length(), req.getLength());
        StrengthLevel strength = entropyService.strengthLevel(entropy);
        String       crack    = entropyService.estimateCrackTime(entropy);

        PasswordEntry entry = new PasswordEntry();
        entry.setPassword(password);
        entry.setEntropyBits(Math.round(entropy * 100.0) / 100.0);
        entry.setStrengthLevel(strength);
        entry.setCrackTimeEstimate(crack);
        entry.setCharsetSize(charset.length());
        entry.setBcryptHash(bcrypt.encode(password));
        return entry;
    }

    String buildCharset(PasswordRequest req) {
        StringBuilder sb = new StringBuilder();
        if (req.isUseUppercase())    sb.append(UPPERCASE);
        if (req.isUseLowercase())    sb.append(LOWERCASE);
        if (req.isUseNumbers())      sb.append(NUMBERS);
        if (req.isUseSpecialChars()) sb.append(SPECIAL);

        String charset = sb.toString();
        if (req.isExcludeAmbiguous()) {
            for (char c : AMBIGUOUS.toCharArray()) {
                charset = charset.replace(String.valueOf(c), "");
            }
        }
        return charset;
    }

    /**
     * Generate password with guaranteed character-set compliance + Fisher-Yates shuffle.
     */
    private String generateFromCharset(String charset, int length, PasswordRequest req) {
        char[] pwd = new char[length];

        // Collect one required char from each active set
        List<Character> required = new ArrayList<>();
        if (req.isUseUppercase()    && length > required.size())
            required.add(UPPERCASE.charAt(secureRandom.nextInt(UPPERCASE.length())));
        if (req.isUseLowercase()    && length > required.size())
            required.add(LOWERCASE.charAt(secureRandom.nextInt(LOWERCASE.length())));
        if (req.isUseNumbers()      && length > required.size())
            required.add(NUMBERS.charAt(secureRandom.nextInt(NUMBERS.length())));
        if (req.isUseSpecialChars() && length > required.size())
            required.add(SPECIAL.charAt(secureRandom.nextInt(SPECIAL.length())));

        // Place required chars first
        int fill = Math.min(required.size(), length);
        for (int i = 0; i < fill; i++) {
            pwd[i] = required.get(i);
        }

        // Fill the rest randomly
        for (int i = fill; i < length; i++) {
            pwd[i] = charset.charAt(secureRandom.nextInt(charset.length()));
        }

        // Fisher-Yates shuffle — critical for security
        for (int i = length - 1; i > 0; i--) {
            int j   = secureRandom.nextInt(i + 1);
            char tmp = pwd[i];
            pwd[i]  = pwd[j];
            pwd[j]  = tmp;
        }

        return new String(pwd);
    }

    // ── Passphrase ────────────────────────────────────────────────────

    private PasswordEntry buildPassphraseEntry(PasswordRequest req) {
        List<String> words = new ArrayList<>();
        for (int i = 0; i < req.getPassphraseWordCount(); i++) {
            words.add(WORDLIST[secureRandom.nextInt(WORDLIST.length)]);
        }
        String passphrase = String.join(req.getPassphraseSeparator(), words);

        double        entropy  = entropyService.calculatePassphraseEntropy(
                                    WORDLIST.length, req.getPassphraseWordCount());
        StrengthLevel strength = entropyService.strengthLevel(entropy);
        String        crack    = entropyService.estimateCrackTime(entropy);

        PasswordEntry entry = new PasswordEntry();
        entry.setPassword(passphrase);
        entry.setEntropyBits(Math.round(entropy * 100.0) / 100.0);
        entry.setStrengthLevel(strength);
        entry.setCrackTimeEstimate(crack);
        entry.setCharsetSize(WORDLIST.length);
        entry.setBcryptHash(bcrypt.encode(passphrase));
        return entry;
    }
}
