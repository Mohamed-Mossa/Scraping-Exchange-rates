package amios;

public class customer {

        private final Long id;
        private final  String Name;


    public customer(Long id, String name) {
        this.id = id;
        Name = name;
    }

    public String getName() {
            return Name;
        }

        public Long getId() {
            return id;
        }

}
