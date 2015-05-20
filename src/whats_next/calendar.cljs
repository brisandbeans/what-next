(ns whats-next.calendar
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [whats-next.state :as state]
            [whats-next.utils :as $]))

(def day-count -21)

(defn day-totals [work]
  (map state/total-duration (state/day-groups-contiguous work)))

(defn task-summary-view [work owner]
  (reify
    om/IRenderState
    (render-state [_ _]
      (let [totals (day-totals work)]
        (dom/ul #js {}
                (dom/li nil "Last 7 days: "
                        ($/pretty-duration
                         (reduce + (take 7 totals))))
                (dom/li nil "Last 30 days: "
                        ($/pretty-duration
                         (reduce + (take 30 totals)))))))))

(defn day-summary-view [work owner]
  (reify
    om/IRender
    (render [_]
      (let [duration (state/total-duration work)]
        (dom/div #js {:className "task-overview"}
                 (count work)
                 " totaling "
                 ($/pretty-duration duration))))))

(defn calendar-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:expanded ($/day-components ($/now))})

    om/IRenderState
    (render-state [_ {:keys [end-date expanded task-type]}]
      (let [month (.getMonth end-date)
            start-date ($/start-of-week ($/inc-date end-date day-count))
            today? ($/same-day? ($/now) end-date)
            work-by-date (state/day-groups
                          (sequence
                           (comp (state/between (.getTime start-date)
                                                (.getTime end-date))
                                 (state/type-filter task-type))
                           (:work app)))]
        (dom/div
         #js {:className "calendar-container"}
         (dom/h3 nil "Task: "
                 (dom/div #js {:className "task-select"}
                          (apply dom/select
                                 #js {:onChange #(om/set-state! owner :task-type
                                                                (.. % -target -value))
                                      :value task-type}
                                 (for [{n :name} (:task-types app)]
                                   (dom/option #js {:value n} n)))))
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
                                                   (when work
                                                     "worked"))
                                   :href "#"
                                   :onClick #(om/set-state! owner :expanded dc)}
                              (dom/div #js {:className "date"}
                                       (.getDate date)))))))



         (when expanded
           (om/build day-summary-view (work-by-date expanded)))

         (om/build task-summary-view (sequence (comp
                                                (state/before (.getTime end-date))
                                                (state/type-filter task-type))
                                               (:work app))))))))
