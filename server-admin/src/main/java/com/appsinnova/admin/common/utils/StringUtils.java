package com.appsinnova.admin.common.utils;

import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils extends org.apache.commons.lang3.StringUtils {
    private static final Set<Character> PUNCTUATION = new HashSet<>();
    static {
        // 添加常见标点符号（可根据需求扩展）
        PUNCTUATION.add(',');
        PUNCTUATION.add('.');
        PUNCTUATION.add('!');
        PUNCTUATION.add('?');
        PUNCTUATION.add(';');
        PUNCTUATION.add(':');
        PUNCTUATION.add('，'); // 中文逗号
        PUNCTUATION.add('。'); // 中文句号
        PUNCTUATION.add('！'); // 中文感叹号
        PUNCTUATION.add('？'); // 中文问号
    }

    /**
     * 生成字符串的32位md5
     * @param str
     * @return
     */
    public static String getMD5(String str) {
        try {
            // 生成一个MD5加密计算摘要
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 计算md5函数
            md.update(str.getBytes());
            // hash()最后确定返回md5 hash值，返回值为8为字符串。因为md5 hash值是16位的hex值，实际上就是8位的字符
            // BigInteger函数则将8位的字符串转换成16位hex值，用字符串来表示；得到字符串形式的hash值
            String md5 = new BigInteger(1, md.digest()).toString(16);
            int length = 32;
            String zerostr = "";
            for (int i = 0; i < length - md5.length(); i++) {
                zerostr += "0";
            }
            return zerostr + md5;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "00000000000000000000000000000000";
    }

    /**
     * 检查当前空格后是否紧跟标点符号
     * @param chars 字符数组
     * @param index 当前字符的索引
     * @return 如果后一个字符是标点符号，返回 true；否则返回 false
     */
    private static boolean isPunctuationNext(char[] chars, int index) {
        if (index + 1 >= chars.length) {
            return false;
        }
        char nextChar = chars[index + 1];
        return PUNCTUATION.contains(nextChar);
    }

    /**
     * 处理字符串中的空格，满足以下条件：
     * 1. 非空格字符之间最多只保留一个空格；
     * 2. 非标点符号与标点符号之间不能有空格。
     * @param input 输入字符串
     * @return 处理后的字符串
     */
    public static String processSpaces(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // 定义全角空格和半角空格
        char fullWidthSpace = '　'; // 全角空格
        char halfWidthSpace = ' ';  // 半角空格

        // 将字符串转换为字符数组
        char[] chars = input.toCharArray();
        StringBuilder result = new StringBuilder();

        // 标记是否已经添加了一个空格
        boolean spaceAdded = false;

        for (int i = 0; i < chars.length; i++) {
            char currentChar = chars[i];

            // 如果是全角空格或半角空格
            if (currentChar == fullWidthSpace || currentChar == halfWidthSpace) {
                // 检查是否需要保留空格
                if (!spaceAdded && !isPunctuationNext(chars, i)) {
                    result.append(halfWidthSpace);
                    spaceAdded = true;
                }
            } else {
                // 如果是非空格字符，直接追加
                result.append(currentChar);
                spaceAdded = false;
            }
        }

        return result.toString();
    }

    /**
     * 返回最后一个段落的末尾 maxLen 个字符
     * @param text 输入文本（用 \n 分隔段落）
     * @return 最后一个段落的末尾 maxLen 个字符（如果段落长度小于 maxLen，则返回整个段落）
     */
    public static String getLastParagraphEnd(String text, int maxLen) {
        if (isBlank(text)) {
            return "";
        }

        // 1. 按 \n 分割段落
        String[] paragraphs = text.split("\n");

        // 2. 获取最后一个非空段落
        String lastParagraph = "";
        for (int i = paragraphs.length - 1; i >= 0; i--) {
            String paragraph = paragraphs[i].trim();
            if (isNotBlank(paragraph) && !paragraph.startsWith("<image src")) {
                lastParagraph = paragraph;
                break;
            }
        }

        if (isBlank(lastParagraph)) {
            return "";
        }

        // 3. 处理内容中的空格
        lastParagraph = processSpaces(lastParagraph);

        // 4. 截取末尾信息
        int length = lastParagraph.length();
        int startIndex = Math.max(0, length - maxLen); // 确保 startIndex 不小于 0
        return lastParagraph.substring(startIndex);
    }

    public static List<String> doParseOptionalExpression(String expression) {
        List<String> result = new ArrayList<>();
        int pos = 0;
        int startIndex = 0;
        int endIndex = 0;
        boolean inMatch = false;
        boolean hasOptional = false;
        while (pos < expression.length()) {
            char ch = expression.charAt(pos);
            if (ch == '{') {
                startIndex = pos;
                inMatch = true;
            } else if (inMatch && '}' == ch) {
                endIndex = pos;
                hasOptional = true;
                break;
            }
            pos++;
        }
        if (!hasOptional) {
            result.add(expression);
            return result;
        }
        String prefix = expression.substring(0, startIndex);
        String optionalstr = expression.substring(startIndex + 1, endIndex);
        String suffix = expression.substring(endIndex + 1);
        String[] options = optionalstr.split("\\|");
        if (options == null || options.length == 0) {
            result.add(prefix + suffix);
        } else {
            for (int i = 0; i < options.length; i++) {
                String resultoption = prefix + options[i] + suffix;
                if (!result.contains(resultoption)) {
                    result.add(resultoption);
                }
            }
        }
        return result;
    }

    public static boolean isLetterDigit(String str) {
        String regex = "^[a-z0-9A-Z ]+$";
        return str.matches(regex);
    }

    public static boolean isLetterDigitV2(String str) {
        String regex = "^[a-z0-9A-Z.'\\-& ]+$";
        return str.matches(regex);
    }

    public static boolean isBigLetterDigit(String str) {
        String regex = "^[0-9A-Z ]+$";
        return str.matches(regex);
    }

    /**
     * 判断邮箱
     * @param email
     * @return
     */
    public static boolean checkMail(String email) {
        //String emailRegex = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        Pattern regex = Pattern.compile(emailRegex);
        Matcher matcher = regex.matcher(email);
        return matcher.matches();
    }

    /**
     * 判断字符串是否包含连续xx个数字
     * @param input
     * @param count
     * @return
     */
    public static boolean containsRowDigits(String input, int count) {
        // 定义连续10个数字的正则表达式模式
        String pattern = "\\d{" + count +"}";

        // 使用matches方法判断是否包含连续count个数字
        return input.matches(".*" + pattern + ".*");
    }

    /**
     * 判断出现2组重复字母
     * @param input
     * @return
     */
    public static boolean containsDuplicateLetters(String input) {
        // 定义重复字母的正则表达式模式
        String pattern = "(\\w)\\1.*?(\\w)\\2";

        // 创建Pattern对象
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(input);
        return matcher.find();
    }

    /**
     * 判断出现3个重复字母
     * @param input
     * @return
     */
    public static boolean containsRepeatedLetter(String input) {
        // 定义重复字母的正则表达式模式
        String pattern = "(\\w)\\1{2,}";

        // 创建Pattern对象
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(input);
        return matcher.find();
    }

    /**
     * 判断字符串只包含一个单词
     * @param input
     * @return
     */
    public static boolean containsOnlyOneWord(String input) {
        // 构建正则表达式模式，使用\\b表示单词边界
        String pattern = "\\b\\w+\\b";

        // 使用正则表达式匹配整个字符串
        return Pattern.matches(pattern, input);
    }

    public static String spitRedisKey(String key) {
        String [] array = key.split("_");
        if (array.length == 1) {
            return key;
        }

        String result = array[0] + "_";
        for (int i=1; i<array.length; i++) {
            String tmp = array[i];
            if (tmp.matches("^\\d.*")) {
                break;
            } else if (i >= 3 && tmp.contains("-")) {
                break;
            }
            result += tmp;
            result += "_";
        }
        return result;
    }

    public static void main(String[] args) {
        String tmpBuf = "{p}{sd-embed class=\\\"sd-embedded-media\\\" data-embed-file=\\\"%3Clink href=%22/resources/stylesheets/bankrate/bankrate.min.css%22 rel=%22stylesheet%22/%3E%3Cimg alt=%22%22 height=%221%22 src=%22https://navi.cohesionapps.com/partner%3Fevent=pageview%26amp%3Bwrite_key=wk_1NT28pl9mcyTc0an54g3qHgqeHb%26amp%3Burl=aHR0cHM6Ly93d3cuYmFua3JhdGUuY29tL2ludmVzdGluZy9jcnlwdG8tZGVmaW5pdGlvbnMv%26amp%3Breferrer=aHR0cHM6Ly9zaW5jbGFpci1iYW5rcmF0ZS1zeW5kaWNhdGVkY29udGVudC5jb20%26amp%3Btitle=Explainer: What common cryptocurrency terms mean%26amp%3BbusinessContext=eyJjdXN0b21TdHJpbmcxIjogIntcImNhbXBhaWduXCI6IFwic2luY2xhaXItaW52ZXN0aW5nLXN5bmRpY2F0aW9uLWZlZWRcIiwgXCJtZWRpdW1cIjogXCJyc3NcIn0ifQ==%26amp%3Bua=0%26amp%3Bip=0%22 style=%22display:none%22 width=%221%22/%3E%3Cp%3ECryptocurrency can be an exciting opportunity for investors%2C and trading digital currency is increasingly mainstream. But %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/what-is-cryptocurrency/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%22 rel=%22sponsored%22 target=%22_blank%22%3Ecryptocurrency%3C/a%3E can feel complex and overwhelming if you%E2%80%99re not familiar with the terminology.%3C/p%3E%3Cp%3EHere%E2%80%99s a rundown of the most common cryptocurrency terms so you can make informed decisions as you explore the crypto landscape.%3C/p%3E%3Ch2%3EWhat is cryptocurrency%3F%3C/h2%3E%3Cp%3ECryptocurrency is a type of digital or virtual currency that uses cryptography for security.%C2%A0%3C/p%3E%3Cdiv data-frame=%22script%22 id=%22myfinance-87642cbf-b3af-4ff5-b509-c1f774d0e0c8%22 style=%22width: 100%25%3B margin: auto%3B box-sizing: border-box%3B position: relative%3B%22%3E%3Cscript src=%22https://static.myfinance.com/embed/myfiFrame.js%3Fcampaign=sinclair-investing-syndication-feed%26amp%3Bmedium=embedded%26amp%3Bplacement=53059f4f12%26amp%3Breferrer=https://sinclair-bankrate-syndicatedcontent.com%26amp%3BscriptDomain=https://www.myfinance.com%26amp%3Bselector=myfinance-87642cbf-b3af-4ff5-b509-c1f774d0e0c8%26amp%3Bwidget=f9fc77bf-76a4-4c72-8748-1d7567b5b450%26amp%3Bwidth=0%22 type=%22text/javascript%22%3E%3C/script%3E%3C/div%3E%3Cp%3EUnlike traditional currencies issued by governments%2C like dollars or euros%2C cryptocurrencies operate on a decentralized system called blockchain %E2%80%94 a distributed ledger that records all transactions across a network of computers. Blockchain removes financial middlemen %E2%80%94 traditional institutions%2C like banks %E2%80%94%C2%A0 while ensuring the security of transactions.%3C/p%3E%3Cp%3ECryptocurrencies can serve different functions depending on their design.%C2%A0%3C/p%3E%3Cp%3EFor example%2C %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/what-is-bitcoin/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%22 rel=%22sponsored%22 target=%22_blank%22%3EBitcoin%3C/a%3E was created to enable money transfers%2C though it%E2%80%99s rarely accepted as currency and operates slower than many payment systems. %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/what-is-ethereum/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%22 rel=%22sponsored%22 target=%22_blank%22%3EEthereum%3C/a%3E%2C on the other hand%2C powers %E2%80%9Csmart contracts%E2%80%9D that self-execute under set conditions. %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/what-is-dogecoin/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%22 rel=%22sponsored%22 target=%22_blank%22%3EDogecoin%3C/a%3E%2C meanwhile%2C began as a playful take on Bitcoin.%3C/p%3E%3Cp%3EWhile some cryptocurrencies serve specific purposes%2C many people use them for speculation. Most people trade coins in hopes of profiting from price swings rather than for any underlying fundamentals. For many%2C this speculative play is cryptocurrency%E2%80%99s biggest draw.%3C/p%3E%3Ch2%3ECommon cryptocurrency terms defined%C2%A0%3C/h2%3E%3Cp%3EHere is a list of the most common cryptocurrency terms.%C2%A0%3C/p%3E%3Cdiv data-frame=%22script%22 id=%22myfinance-dd5d043b-6e56-4a0d-baa4-991f5b8581b9%22 style=%22width: 100%25%3B margin: auto%3B box-sizing: border-box%3B position: relative%3B%22%3E%3Cscript src=%22https://static.myfinance.com/embed/myfiFrame.js%3Fcampaign=sinclair-investing-syndication-feed%26amp%3Bmedium=embedded%26amp%3Bplacement=a3b81b3561%26amp%3Breferrer=https://sinclair-bankrate-syndicatedcontent.com%26amp%3BscriptDomain=https://www.myfinance.com%26amp%3Bselector=myfinance-dd5d043b-6e56-4a0d-baa4-991f5b8581b9%26amp%3Bwidget=f9fc77bf-76a4-4c72-8748-1d7567b5b450%26amp%3Bwidth=0%22 type=%22text/javascript%22%3E%3C/script%3E%3C/div%3E%3Cp%3E%3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/crypto-definitions/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%23A%22 rel=%22sponsored%22 target=%22_blank%22%3EA%3C/a%3E %7C %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/crypto-definitions/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%23B%22 rel=%22sponsored%22 target=%22_blank%22%3EB%3C/a%3E %7C %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/crypto-definitions/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%23C%22 rel=%22sponsored%22 target=%22_blank%22%3EC%3C/a%3E %7C %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/crypto-definitions/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%23D%22 rel=%22sponsored%22 target=%22_blank%22%3ED%3C/a%3E %7C %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/crypto-definitions/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%23F%22 rel=%22sponsored%22 target=%22_blank%22%3EF%3C/a%3E %7C %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/crypto-definitions/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%23G%22 rel=%22sponsored%22 target=%22_blank%22%3EG%3C/a%3E %7C %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/crypto-definitions/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%23H%22 rel=%22sponsored%22 target=%22_blank%22%3EH%3C/a%3E %7C %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/crypto-definitions/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%23I%22 rel=%22sponsored%22 target=%22_blank%22%3EI%3C/a%3E %7C %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/crypto-definitions/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%23L%22 rel=%22sponsored%22 target=%22_blank%22%3EL%3C/a%3E %7C %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/crypto-definitions/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%23M%22 rel=%22sponsored%22 target=%22_blank%22%3EM%3C/a%3E %7C %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/crypto-definitions/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%23N%22 rel=%22sponsored%22 target=%22_blank%22%3EN%3C/a%3E %7C %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/crypto-definitions/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%23P%22 rel=%22sponsored%22 target=%22_blank%22%3EP%3C/a%3E %7C %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/crypto-definitions/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%23S%22 rel=%22sponsored%22 target=%22_blank%22%3ES%3C/a%3E %7C %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/crypto-definitions/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%23T%22 rel=%22sponsored%22 target=%22_blank%22%3ET%3C/a%3E %7C %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/crypto-definitions/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%23W%22 rel=%22sponsored%22 target=%22_blank%22%3EW%3C/a%3E%3C/p%3E%3Ch3%3E%3Ch3 id=%22A%22%3EAltcoin%3C/h3%3E%3C/h3%3E%3Cp%3EAny cryptocurrency that is not Bitcoin. Some sources estimate the number of individual %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/what-are-altcoins/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%22 rel=%22sponsored%22 target=%22_blank%22%3Ealtcoins%3C/a%3E at about 15%2C000.%3C/p%3E%3Ch3%3E%3Ch3 id=%22B%22%3EBlockchain%3C/h3%3E%3C/h3%3E%3Cp%3EThe technology underpinning cryptocurrency%2C enabling it to exist as a secure method of moving and validating transactions and information. It%E2%80%99s a kind of database that tracks and secures data in blocks and then chains them together chronologically. %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/what-is-blockchain/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%22 rel=%22sponsored%22 target=%22_blank%22%3EBlockchain%3C/a%3E is sometimes referred to as a digital ledger.%C2%A0%3C/p%3E%3Ch3%3EBTC%3C/h3%3E%3Cp%3ECommon symbol for the Bitcoin cryptocurrency.%3C/p%3E%3Ch3%3E%3Ch3 id=%22C%22%3ECentralized exchange%C2%A0%3C/h3%3E%3C/h3%3E%3Cp%3EA type of %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/best-crypto-exchanges-and-trading-apps/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%22 rel=%22sponsored%22 target=%22_blank%22%3Ecrypto exchange%3C/a%3E where trades are processed by a company or organization. Coinbase and Kraken are two popular centralized exchanges.%C2%A0%3C/p%3E%3Ch3%3ECoin%3C/h3%3E%3Cp%3EA digital asset that operates independently on its own blockchain. For example%2C Bitcoin is the coin for the Bitcoin blockchain%2C and Ether is the coin for the Ethereum blockchain.%3C/p%3E%3Ch3%3ECold storage%3C/h3%3E%3Cp%3EA method of %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/what-is-a-cold-wallet/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%22 rel=%22sponsored%22 target=%22_blank%22%3Estoring cryptocurrencies offline%3C/a%3E to increase security.%3C/p%3E%3Ch3%3E%3Ch3 id=%22D%22%3EDecentralized exchange%3C/h3%3E%3C/h3%3E%3Cp%3EA type of crypto exchange that operates without a central authority.%3C/p%3E%3Ch3%3EDecentralized finance (DeFi)%3C/h3%3E%3Cp%3EDeFi %E2%80%94 short for %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/what-is-decentralized-finance-defi-crypto/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%22 rel=%22sponsored%22 target=%22_blank%22%3Edecentralized finance%3C/a%3E %E2%80%94 is a financial system based on peer-to-peer payments through blockchain technology. Via blockchain%2C DeFi lets users sidestep traditional financial middlemen like banks or brokers. DeFi can cover a wide range of financial activities%2C including payments%2C investments and lending.%3C/p%3E%3Ch3%3E%3Ch3 id=%22F%22%3EFiat currency%3C/h3%3E%3C/h3%3E%3Cp%3EGovernment-issued currency not backed by physical commodities%2C such as gold. The value of fiat currency is supported by the issuing government and its economic strength.%3C/p%3E%3Ch3%3E%3Ch3 id=%22G%22%3EGas fee%3C/h3%3E%3C/h3%3E%3Cp%3EThe cost required to perform a transaction or execute a smart contract on the Ethereum network.%C2%A0%3C/p%3E%3Ch3%3E%3Ch3 id=%22H%22%3EHalving%3C/h3%3E%3C/h3%3E%3Cp%3EA %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/bitcoin-halving-what-does-it-mean-for-investors/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%22 rel=%22sponsored%22 target=%22_blank%22%3Ehalving%3C/a%3E is a process that cuts the mining rewards in half roughly every four years to reduce the issuance rate of Bitcoin. (New Bitcoins are issued when high-powered computers called Bitcoin miners process complex math problems.)%3C/p%3E%3Ch3%3E%3Ch3 id=%22I%22%3EICO%3C/h3%3E%3C/h3%3E%3Cp%3EInitial coin offering. Similar to an %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/getting-in-on-an-initial-public-offering/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%22 rel=%22sponsored%22 target=%22_blank%22%3Einitial public offering%3C/a%3E (IPO) for stocks%2C an ICO is a fundraising method used by cryptocurrency projects.%3C/p%3E%3Ch3%3E%3Ch3 id=%22L%22%3ELedger%3C/h3%3E%3C/h3%3E%3Cp%3EA record of all cryptocurrency transactions on the blockchain.%3C/p%3E%3Ch3%3E%3Ch3 id=%22M%22%3EMemecoin%3C/h3%3E%3C/h3%3E%3Cp%3EA type of cryptocurrency inspired by internet memes%2C pop culture or social media trends. Unlike mainstream cryptocurrencies like Bitcoin or Ethereum%2C %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/what-are-memecoins-in-crypto/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%22 rel=%22sponsored%22 target=%22_blank%22%3Ememecoins%3C/a%3E often lack a clear utility or technological innovation. Instead%2C their value is largely driven by online hype and humor. Examples include %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/what-is-dogecoin/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%22 rel=%22sponsored%22 target=%22_blank%22%3EDogecoin%3C/a%3E and %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/what-is-shiba-inu-shib-cryptocurrency/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%22 rel=%22sponsored%22 target=%22_blank%22%3EShiba Inu%3C/a%3E.%C2%A0%3C/p%3E%3Ch3%3EMining%3C/h3%3E%3Cp%3EThe process of verifying and adding transactions to a blockchain. Successfully %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/what-is-bitcoin-mining/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%22 rel=%22sponsored%22 target=%22_blank%22%3EBitcoin mining%3C/a%3E rewards miners with a predetermined amount of Bitcoin.%3C/p%3E%3Ch3%3E%3Ch3 id=%22N%22%3ENFT%3C/h3%3E%3C/h3%3E%3Cp%3ENon-fungible token. Built on blockchain technology%2C %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/what-is-an-nft-non-fungible-token/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%22 rel=%22sponsored%22 target=%22_blank%22%3ENFTs%3C/a%3E provide a secure and transparent way to record ownership of digital assets. NFT transactions are permanently recorded%2C making it nearly impossible to counterfeit or dispute ownership. A NFT can represent various digital products%2C such as artwork%2C collectibles%2C video clips%2C in-game items or music albums.%3C/p%3E%3Ch3%3E%3Ch3 id=%22P%22%3EProof of stake (PoS)%3C/h3%3E%3C/h3%3E%3Cp%3EIn a proof-of-stake system%2C cryptocurrency holders %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/crypto-staking/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%23what-is-staking%22 rel=%22sponsored%22 target=%22_blank%22%3Ecan stake their coins%3C/a%3E to validate transactions and earn rewards. To become a validator%2C a minimum amount of coins is required. Validators ensure the security and integrity of the blockchain. Those who don%E2%80%99t meet the minimum staking requirement can delegate their coins to validators and earn rewards that way. While many popular cryptocurrencies%2C such as Ethereum%2C use proof-of-stake validation%2C not all do.%C2%A0%3C/p%3E%3Ch3%3EProof of work (PoW)%3C/h3%3E%3Cp%3EProof of work uses a process known as mining to validate transactions and manage that coin%E2%80%99s blockchain. The first miner to solve a puzzle adds a new block of transactions to the blockchain and is rewarded with cryptocurrency. Bitcoin uses a proof-of-work system%2C a process that consumes significant energy and resources.%C2%A0%C2%A0%C2%A0%3C/p%3E%3Ch3%3E%3Ch3 id=%22S%22%3ESmart contract%3C/h3%3E%3C/h3%3E%3Cp%3ESelf-executing contracts with the terms of the agreement directly written into code. Built on blockchain technology%2C %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/what-is-a-smart-contract-crypto/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%22 rel=%22sponsored%22 target=%22_blank%22%3Esmart contracts%3C/a%3E automatically fulfill their obligations once specific conditions are met. This transparency and automation eliminates the need for intermediaries%2C like banks%2C reducing the risk of errors or disputes.%C2%A0%3C/p%3E%3Ch3%3EStablecoin%3C/h3%3E%3Cp%3EA type of cryptocurrency designed to maintain a stable value%2C often pegged to a fiat currency like the U.S. dollar. To achieve this%2C %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/stablecoin-cryptocurrency/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%22 rel=%22sponsored%22 target=%22_blank%22%3Establecoins%3C/a%3E often rely on backing assets like U.S. dollars%2C or they use algorithmic mechanisms to adjust supply and demand. Unlike highly volatile cryptocurrencies%2C stablecoins are meant to offer a more predictable and reliable store of value.%C2%A0%3C/p%3E%3Ch3%3EStaking%3C/h3%3E%3Cp%3EThe process of locking up funds in a cryptocurrency to support the network and earn rewards.%3C/p%3E%3Ch3%3E%3Ch3 id=%22T%22%3EToken%3C/h3%3E%3C/h3%3E%3Cp%3EA digital asset representing ownership of a specific project or service.%3C/p%3E%3Ch3%3E%3Ch3 id=%22W%22%3EWallet%3C/h3%3E%3C/h3%3E%3Cp%3EA %3Ca data-trans-lid=%2226736523%22 href=%22https://www.bankrate.com/investing/what-is-a-crypto-wallet/%3Fmf_ct_campaign=sinclair-investing-syndication-feed%26amp%3Butm_content=syndication%23what-is-a-cryptocurrency-wallet%22 rel=%22sponsored%22 target=%22_blank%22%3Ecryptocurrency wallet%3C/a%3E is a device used to store and manage crypto holdings. It safeguards private keys%2C which are essential for accessing and controlling your coins. These wallets can be either software-based or hardware-based. Hardware wallets (cold wallets) operate offline. They%E2%80%99re the most secure but carry the risk and responsibility of self-custody. Software wallets (hot wallets)%2C are more accessible but are less secure and require strong security measures to protect against potential hacks.%3C/p%3E%3Cscript async=%22%22 src=%22https://static.myfinance.com/embed/myFinanceRss.js%22 type=%22text/javascript%22%3E%3C/script%3E\\\" data-embed-type=\\\"code\\\" contenteditable=\\\"false\\\"}{/sd-embed}{/p}";

        int idx1 = tmpBuf.indexOf("data-embed-file=\\\"");
        tmpBuf = tmpBuf.substring(idx1+"data-embed-file=\\\"".length());

        int idx2 = tmpBuf.indexOf("\\\" data-embed-type=");
        tmpBuf = tmpBuf.substring(0, idx2);

        try {
            String htmlText = URLDecoder.decode(tmpBuf, "UTF-8");
            return;
        } catch (Exception e) {
            return;
        }


/*        String key = "news_googleRealtime_V2_fr-FR_Top stories_0D6317B818CBE8944813D4664368DAA2";
        String tmp = spitRedisKey(key);

        String sss = "test my face!";
        boolean bret = isLetterDigit(sss);

        String bigString = "DFB9D342C8009D9EBCF56F837DBC93F6";
        boolean bBigRet = isBigLetterDigit(bigString);
        return;*/
    }

    public static int sscanf(String input, String format, @SuppressWarnings("rawtypes") Collection export) {
        return sscanf(input, 0, format, export);
    }

    public static int sscanf(String input, int startIndex, String format,
                             @SuppressWarnings("rawtypes") Collection export) {
        if (format.indexOf('%') < 0) {
            return -1;
        }
        // 分解格式
        List<String> subFormats = new ArrayList<>();
        if (!resolveFormatString(subFormats, format)) {
            return -1;
        }
        // 解析输入
        try {
            return resolveInputString(subFormats, format, export, input, startIndex);
        } catch (NullPointerException e) {
            return -1;
        }
    }

    @SuppressWarnings("unchecked")
    private static int resolveInputString(List<String> subFormats, String format,
                                          @SuppressWarnings("rawtypes") Collection export, String input, int inputIndex) {
        StringWriter writer = null;
        for (int formatIndex = 0; formatIndex < subFormats.size(); ++formatIndex) {
            String subFormat = subFormats.get(formatIndex);
            if ("%u".equals(subFormat)) {
                for (; inputIndex < input.length(); ++inputIndex) {
                    char c = input.charAt(inputIndex);
                    if ('9' < c || '0' > c) {
                        if (writer == null) {
                            continue;
                        } else {
                            break;
                        }
                    }
                    if (writer == null) {
                        writer = new StringWriter(input.length());
                    }
                    writer.append(c);
                }
                long integer = Long.parseLong(writer.toString());
                writer = null;
                export.add(integer);
            } else if ("%d".equals(subFormat) || "%l".equals(subFormat)) {
                boolean isNeg = false;
                for (; inputIndex < input.length(); ++inputIndex) {
                    char c = input.charAt(inputIndex);
                    if ('9' < c || '0' > c) {
                        if (writer == null) {
                            if (c == '-') {
                                isNeg = true;
                            } else {
                                isNeg = false;
                            }
                            continue;
                        } else {
                            break;
                        }
                    }
                    if (writer == null) {
                        writer = new StringWriter(input.length());
                    }
                    writer.append(c);
                }
                long integer = Long.parseLong(writer.toString());
                if (isNeg) {
                    integer = -integer;
                }
                writer = null;
                export.add(integer);
            } else if ("%f".equals(subFormat)) {
                boolean isNeg = false;
                int dotCount = 0;
                for (; inputIndex < input.length(); ++inputIndex) {
                    char c = input.charAt(inputIndex);
                    if (c == '.') {
                        ++dotCount;
                        if (dotCount > 1) {
                            break;
                        }
                    } else if ('9' < c || '0' > c) {
                        if (writer == null) {
                            if (c == '-') {
                                isNeg = true;
                            } else {
                                isNeg = false;
                            }
                            continue;
                        } else {
                            break;
                        }
                    }
                    if (writer == null) {
                        writer = new StringWriter(input.length());
                    }
                    writer.append(c);
                }
                double integer = Double.parseDouble(writer.toString());
                if (isNeg) {
                    integer = -integer;
                }
                writer = null;
                export.add(integer);
            } else if ("%c".equals(subFormat)) {
                char c = input.charAt(inputIndex);
                export.add(c);
                ++inputIndex;
            } else if ("%s".equals(subFormat)) {
                if (formatIndex >= subFormats.size() - 1) {
                    export.add(input.substring(inputIndex));
                    inputIndex = input.length();
                    continue;
                }
                String subFormat2 = subFormats.get(formatIndex + 1);
                if ("%u".equals(subFormat2)) {
                    for (; inputIndex < input.length(); ++inputIndex) {
                        char c = input.charAt(inputIndex);
                        if (writer == null) {
                            writer = new StringWriter(input.length());
                        }
                        if ('9' < c || '0' > c) {
                            writer.append(c);
                        } else {
                            break;
                        }
                    }
                    export.add(writer.toString());
                    writer = null;
                } else if ("%d".equals(subFormat2) || "%l".equals(subFormat2)) {
                    for (; inputIndex < input.length(); ++inputIndex) {
                        char c = input.charAt(inputIndex);
                        if (writer == null) {
                            writer = new StringWriter(input.length());
                        }
                        if ('9' < c || '0' > c) {
                            writer.append(c);
                        } else {
                            break;
                        }
                    }
                    export.add(writer.toString());
                    writer = null;
                } else if ("%f".equals(subFormat2)) {
                    for (; inputIndex < input.length(); ++inputIndex) {
                        char c = input.charAt(inputIndex);
                        if (writer == null) {
                            writer = new StringWriter(input.length());
                        }
                        if (('9' < c || '0' > c) /* && '.' != c */) {
                            writer.append(c);
                        } else {
                            break;
                        }
                    }
                    export.add(writer.toString());
                    writer = null;
                } else if ("%s".equals(subFormat2) || "%c".equals(subFormat2)) {
                    return -1;
                } else {
                    int nextInputIndex = input.indexOf(subFormat2, inputIndex);
                    if (nextInputIndex < 0) {
                        nextInputIndex = input.length();
                    }
                    String skipString = input.substring(inputIndex, nextInputIndex);
                    inputIndex = nextInputIndex + subFormat2.length();
                    export.add(skipString);
                    ++formatIndex;
                }
            } else {
                int searchIndex = input.indexOf(subFormat, inputIndex);
                if (searchIndex < 0) {
                    return -1;
                }
                inputIndex = searchIndex + subFormat.length();
            }
        }
        return inputIndex;
    }

    private static boolean resolveFormatString(List<String> subFormats, String format) {
        // 分解格式
        StringWriter writer = null;
        for (int index = 0; index < format.length(); ++index) {
            char c = format.charAt(index);
            // 普通字符
            if (c != '%') {
                if (writer == null) {
                    writer = new StringWriter(format.length());
                }
                writer.append(c);
                continue;
            }
            // 结束判定
            if (index == format.length() - 1) {
                writer.append(c);
                break;
            }
            // 下一个字符
            ++index;
            char c2 = format.charAt(index);
            // 发现%
            if (c2 == '%') {
                if (writer == null) {
                    writer = new StringWriter(format.length());
                }
                writer.append(c);
                continue;
            }
            // 格式判定
            if ("udflsc".indexOf(c2) < 0) {
                return false;
            }
            if (writer != null) {
                subFormats.add(writer.toString());
                writer = null;
            }
            subFormats.add("%" + c2);
        }
        if (writer != null)

        {
            subFormats.add(writer.toString());
        }
        return true;
    }

    public static String convertSpace(String str) {
        if (str == null) {
            return "";
        }
        str = str.replace("\u00A0", " "); // nbsp，非断行空格
        str = str.replace("\u200B", ""); // 零宽空格
        return str;
    }

    public static boolean isBlank(CharSequence cs) {
        if (cs == null) {
            return true;
        }
        String str = convertSpace(cs.toString());
        return org.apache.commons.lang3.StringUtils.isBlank(str);
    }

    /**
     * 按ASCII值对字符串进行排序。
     *
     * @param input 要排序的输入字符串
     * @return 返回按ASCII值排序后的字符串
     */
    public static String sortStringByAscii(String input) {
        // 如果输入字符串为null，直接返回null
        if (input == null) {
            return null;
        }

        // 将输入字符串转换为字符数组
        char[] charArray = input.toCharArray();

        // 使用Arrays.sort方法对字符数组进行排序
        Arrays.sort(charArray);

        // 将排序后的字符数组转换回字符串并返回
        return new String(charArray);
    }


    /**
     * 获取字符串中第一次出现的数字的索引
     * @param str
     * @return
     */
    public static int indexOfNum(String str) {
        if (str == null || "".equals(str)) {
            return -1;
        }
        for (int i = 0; i < str.length(); i++) {
            if (Character.isDigit(str.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    static public List<String> string2List(String value) {
        List<String> valueList = new ArrayList<>();
        if (StringUtils.isNotEmpty(value)) {
            try {
                String [] array = value.split(",");
                for (String s : array) {
                    s = s.trim();
                    if (StringUtils.isNotBlank(s)) {
                        valueList.add(s);
                    }
                }
            } catch (Exception e) {
                return valueList;
            }
        }
        return valueList;
    }

    static public List<Long> string2LongList(String value) {
        List<Long> valueList = new ArrayList<>();
        if (StringUtils.isNotEmpty(value)) {
            try {
                String [] array = value.split(",");
                for (String s : array) {
                    s = s.trim();
                    if (StringUtils.isNotBlank(s)) {
                        valueList.add(Long.valueOf(s));
                    }
                }
            } catch (Exception e) {
                return valueList;
            }
        }
        return valueList;
    }

    public static <T> String list2String(List<T> valueList){
        if (valueList == null || valueList.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (T item : valueList) {
            sb.append(item.toString()).append(",");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public static <T> String set2String(Set<T> valueSet){
        if (valueSet == null || valueSet.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (T item : valueSet) {
            sb.append(item.toString()).append(",");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public static Integer toInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return null;
        }
    }

    public static Long toLong(String str) {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            return null;
        }
    }

    public static String[] splitCurrency(String currencyStr) {
        // 定义正则表达式：匹配货币单位和数值部分
        String regex = "([^\\d.,]+)([\\d.,]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(currencyStr);

        if (matcher.find()) {
            // 提取货币单位和数值部分
            String currencyUnit = matcher.group(1).trim(); // 货币单位
            String currencyValue = matcher.group(2).trim(); // 数值部分
            return new String[]{currencyUnit, currencyValue};
        }
        return null;
    }
}
