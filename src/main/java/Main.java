import br.com.sankhya.ce.templating.EvaluteHtml;
import br.com.sankhya.ce.tuples.Pair;

import java.math.BigDecimal;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        EvaluteHtml html = new EvaluteHtml();

        HashMap<BigDecimal, ArrayList<String>> concluidos = new HashMap<>();

        concluidos.put(BigDecimal.ZERO, new ArrayList<>(Arrays.asList("2304987304927498", "4564835096845069")));
        concluidos.put(BigDecimal.ONE, new ArrayList<>(Arrays.asList("3423809809234", "20546949845809")));


        HashMap<BigDecimal, String> erros = new HashMap<>();
        erros.put(BigDecimal.ZERO, "Erro de teste");
        erros.put(BigDecimal.ONE, "Erro de teste");


        String eval = html.eval("/teste.html", Pair.of("concluidos", concluidos), Pair.of("erros", erros));
        System.out.println(eval);
    }

}

