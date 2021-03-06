(ns whats-next.calendar
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [whats-next.state :as state]
            [whats-next.utils :as $]))

(defn day-summary-view [work owner]
  (reify
    om/IRender
    (render [_]
      (let [duration (state/total-duration work)]
        (dom/div #js {:className "task-overview"}
                 (count work)
                 " totaling "
                 ($/pretty-duration duration))))))

;; State properties:
;;  day-count: the number of days to show in the calendar (will be
;;   rounded to show weeks)
;;  transduce-work: transducer that will be applied to the work
;;  classify-day: function that takes a date and the work completed on
;;   that date and returns a string with (a) class(es) to apply to the
;;   corresponding day DOM element
;;  day-contents: function that takes a date and the work completed on
(defn calendar-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:expanded ($/day-components ($/now))})

    om/IRenderState
    (render-state [_ {:keys [day-count end-date expanded
                             transduce-work classify-day]
                      :or {day-count -21}}]
      (let [month (.getMonth end-date)
            start-date ($/start-of-week ($/inc-date end-date day-count))
            today? ($/same-day? ($/now) end-date)
            work-by-date (state/day-groups
                          (sequence
                           (cond->
                            (state/between (.getTime start-date)
                                           (.getTime end-date))

                            transduce-work (comp transduce-work))
                           (:work app)))]
        (dom/div
         #js {:className "calendar-container"}
         (dom/div
          #js {:className "calendar"}
          (dom/div
           #js {:className "col-labels"}
           (for [week-day $/short-day-names]
             (dom/div #js {:className "day-col"} week-day)))
          (for [week (partition-all 7 ($/date-range start-date end-date))]
            (dom/div #js {:className "week"}
                     (for [date week
                           :let [dc ($/day-components date)
                                 work (work-by-date dc)]]
                       (dom/a #js {:className (str "day "
                                                   (when (= dc expanded)
                                                     "selected ")
                                                   (when (= (.getMonth date) month)
                                                     "this-month ")
                                                   (when classify-day
                                                     (classify-day date work)))
                                   :href "#"
                                   :onClick #(om/set-state! owner :expanded dc)}
                              (dom/div #js {:className "date"}
                                       (.getDate date)))))))

         (when expanded
           (om/build day-summary-view (work-by-date expanded))))))))
