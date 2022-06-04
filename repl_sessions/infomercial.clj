(ns my-app.factories
  "When starting out simply create a single `factories` namespace, you can split
  it up later if it gets too big.

  I'll alias `lambdaisland.facai` to `f` for brevity, you can alias to `facai`
  if you prefer to be a bit more explicit.
  "
  (:require [lambdaisland.facai :as f]))

;; Let's define our first factory. You pass it a template of what your data
;; looks like. In this case we simply put a literal map.
(f/defactory user
  {:user/name   "Lilliam Predovic"
   :user/handle "lilli42"
   :user/email  "lilli42@example.com"})

;; We can generate data from this template by calling the factory as a function,
;; which is an alias for calling `f/build-val`

(user) ; or (f/build-val user)
;; => #:user{:name "Lilliam Predovic",
;;           :handle "lilli42",
;;           :email "lilli42@example.com"}

;; It can take a few options, use `:with` to override or supply additional
;; values.

(user {:with {:user/name "Mellissa Schimmel"}})
;; => #:user{:name "Mellissa Schimmel",
;;           :handle "lilli42",
;;           :email "lilli42@example.com"}

;; Functions in the template will be called, and the result in turn is treated
;; as a factory template.

(f/defactory user
  {:user/name   "Lilliam Predovic"
   :user/handle "lilli42"
   :user/email  #(str "lilli" (rand-int 100) "@example.com")})

(user)
;; => #:user{:name "Lilliam Predovic",
;;           :handle "lilli42",
;;           :email "lilli92@example.com"}

;; This means you can decide if you want your factories to be fairly static, or
;; more dynamic and random. Maybe you want all usernames to be unique, the
;; numbered helper can help with that.

(require '[lambdaisland.facai.helpers :as fh])

(f/defactory user
  {:user/name   "Lilliam Predovic"
   :user/handle (fh/numbered
                 #(str "lilli" %))
   :user/email  #(str "lilli" (rand-int 100) "@example.com")})

;; The Faker library can also be useful, especially if you want to for instance
;; populate UIs to take screenshots

(require '[lambdaisland.faker :refer [fake]])

(f/defactory user
  {:user/name   #(fake [:name :name])
   :user/handle #(fake [:internet :username])
   :user/email  #(fake [:internet :email])})

;; Factories can have traits, which are merged into the main template on demand.

(f/defactory article
  {:article/title "7 Tip-top Things To Try"
   :article/status :draft}

  :traits
  {:published {:article/status "published"}
   :unpublished {:article/status "unpublished"}
   :in-the-future {:article/published-at #(fh/days-from-now 2)}
   :in-the-past {:article/published-at #(fh/days-ago 2)}})

(article {:traits [:published :in-the-future]})
;; => #:article{:title "7 Tip-top Things To Try",
;;              :status "published",
;;              :published-at
;;              #object[java.time.ZonedDateTime 0x345be9a1 "2022-03-30T10:09:17.584903790Z[UTC]"]}

;; If you have associations between data, then you simply use the factory as the value. Or if you want to set some specifics with `:with` or `:traits` you can call the factories.

(f/defactory article
  {:article/title "7 Tip-top Things To Try"
   :article/submitter user
   :article/author (user {:with {:user/roles #{"author"}}})})

(article)
;; => #:article{:title "7 Tip-top Things To Try",
;;              :submitter
;;              #:user{:name "Mr. Reinaldo Hartmann",
;;                     :handle "garth",
;;                     :email "ligia@grantgroup.co"},
;;              :author
;;              #:user{:name "Doria Wisoky Sr.",
;;                     :handle "romaine",
;;                     :email "darrin@schummandsons.co",
;;                     :roles #{"author"}}}


;; Note that the actual expansion gets deferred in this case, the `(user)` call
;; will only get expanded when building the `article`, so you get a different
;; username each time.

(article)
;; => #:article{:title "7 Tip-top Things To Try",
;;              :submitter
;;              #:user{:name "Hobert Fadel",
;;                     :handle "kerry91",
;;                     :email "delana57@bednarbednarand.com"},
;;              :author
;;              #:user{:name "Rosemary Reynolds",
;;                     :handle "spencer.bruen",
;;                     :email "laurine@lockmanlockmana.info",
;;                     :roles #{"author"}}}

;; Factories can inherit from other factories. Often it's preferrable to use
;; traits, but this can be a useful feature.


(f/defactory blog-post
  :inherit article
  {:post/uri-slug "/post"})

(blog-post)
;; => {:article/title "7 Tip-top Things To Try",
;;     :article/submitter
;;     #:user{:name "Rima Wintheiser",
;;            :handle "numbers",
;;            :email "leandrowelch@welchwelchandwe.co"},
;;     :article/author
;;     #:user{:name "Jeffrey Bruen DO",
;;            :handle "margy81",
;;            :email "williams.rippin@rippingroup.biz",
;;            :roles #{"author"}},
;;     :post/uri-slug "/post"}

(let [result    (f/build article)
      submitter (f/sel1 result :article/submitter)
      author    (f/sel1 result :article/author)]
  {:article   (f/value result)
   :submitter submitter
   :author    author})

(let [result (f/build article)]
  (f/sel result user))
(article {:rules {[:article/submitter :user/handle] "editosaurus"}})

(set! *print-namespace-maps* false)


(f/defactory organization
  {:name #(str "org" (rand-int 100))})

(f/defactory user
  {:username "jonny"
   :org organization})

(f/defactory department
  {:name "sales"
   :org organization
   :head user})

(f/defactory meeting-room
  {:org organization
   :room-number "123"
   :department department})

(f/defactory booking
  {:start-time #(java.util.Date.)
   :booked-by user
   :room meeting-room})

(booking
 {:rules {[organization] (f/unify)}})


(f/defactory product
  {:sku "123"
   :price 12.99})

(f/defactory product-line-item
  {:product product
   :quantity 1}

  :after-build
  (fn [ctx]
    (f/update-result
     ctx
     (fn [{:as res :keys [product quantity]}]
       (assoc res :total (* (:price product) quantity))))))

(product-line-item);; => {:product {:sku "123", :price 12.99}, :quantity 1, :total 12.99}
(product-line-item {:with {:quantity 2}});; => {:product {:sku "123", :price 12.99}, :quantity 2, :total 25.98}
(product-line-item {:rules {:price 69 :quantity 2}});; => {:product {:sku "123", :price 69}, :quantity 2, :total 138}
