import br.com.sankhya.ce.files.FilesUtils;
import br.com.sankhya.ce.jape.QueryBuilder;
import br.com.sankhya.ce.sql.ResolveNamedParameter;
import br.com.sankhya.ce.sql.ResolveSqlTypes;
import br.com.sankhya.ce.templating.EvaluteHtml;
import br.com.sankhya.ce.tuples.Pair;
import br.com.sankhya.modelcore.facades.DbExplorerSP;
import br.com.sankhya.modelcore.facades.DbExplorerSPBean;
import br.com.sankhya.modelcore.facades.LiberacaoLimitesSPBean;
import br.com.sankhya.ws.transformer.json.Json2XMLParser;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
public class Main {
    public static void main(String[] args) throws Exception {
//        String content = FilesUtils.loadResource("/teste.txt");
//
//        EvaluteHtml evaluteHtml = new EvaluteHtml();
//        Pair<String, Object>[] vars = new Pair[]{Pair.of("num", 10), Pair.of("out", System.out)};
//        content = evaluteHtml.evalJs(content, vars);
//        System.out.println(content);

        String jsonString = "{\n" +
            "    \"serviceName\": \"DbExplorerSP.executeQuery\",\n" +
            "    \"requestBody\": {\n" +
            "        \"sql\": \"SELECT * FROM TGFCAB where NUNOTA<10\"\n" +
            "    }\n" +
            "}";

        // Using Gson instance (recommended for newer Gson versions)
        Gson gson = new Gson();
        JsonParser jp = new JsonParser();


        JsonObject jsonObject = jp.parse(jsonString).getAsJsonObject();

        Element requestBody = Json2XMLParser.jsonToElement("serviceRequest", jsonObject);

        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        String xmlString = xmlOutputter.outputString(requestBody);

        // Exibindo
        System.out.println(xmlString);
        System.out.println(jsonObject);

        //        LiberacaoLimitesSPBean
    }
}
