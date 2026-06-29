import br.com.sankhya.ce.templating.EvaluteHtml;

import java.util.List;

public class Main {

    public static void main(String[] args) {

        EvaluteHtml html = new EvaluteHtml();

        html.addVar("chave", "vRfe_9A87EFC9304A55768CBD6229A668AE2A");
        String eval = html.eval("/teste2.html");

        System.out.println(eval);
    }

    public class Root {
        private List<Root2> root;

        public Root(List<Root2> root) {
            this.root = root;
        }

        public List<Root2> getRoot() {
            return this.root;
        }

        public void setRoot(List<Root2> root) {
            this.root = root;
        }

    }

    public class Root2 {
        private Long userId;
        private Long id;
        private String title;
        private String body;

        public Root2(Long userId, Long id, String title, String body) {
            this.userId = userId;
            this.id = id;
            this.title = title;
            this.body = body;
        }

        public Long getUserId() {
            return this.userId;
        }

        public Long getId() {
            return this.id;
        }

        public String getTitle() {
            return this.title;
        }

        public String getBody() {
            return this.body;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }

}



